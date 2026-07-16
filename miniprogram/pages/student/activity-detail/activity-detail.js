const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const activitiesService = require('../../../services/activities')
const bridge = require('../../../services/bridge')
const api = require('../../../services/api')

Page({
  data: {
    id: '',
    activity: null,
    studentName: '',
    canSignup: false,
    btnText: '立即报名'
  },

  onLoad(query) {
    this.setData({ id: (query && query.id) || '' })
  },

  onShow() {
    if (!auth.requireAuth()) return
    this.refresh()
  },

  async refresh() {
    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)

    if (api.isRemoteSession() && this.data.id) {
      try {
        const raw = await api.fetchActivityDetail(this.data.id)
        const orgCode = (student && student.orgId) || auth.getCurrentOrgId()
        const normalized = api.normalizeActivity(raw, orgCode, studentId)
        // 写回本地，供 decorate 一致
        const list = wx.getStorageSync('xgy_activities') || []
        const idx = list.findIndex((a) => String(a.id) === String(normalized.id))
        if (idx >= 0) list[idx] = { ...list[idx], ...normalized }
        else list.push(normalized)
        wx.setStorageSync('xgy_activities', list)
      } catch (e) {
        // fallback local
      }
    }

    const activity = activitiesService.getById(this.data.id, studentId)
    if (!activity) {
      wx.showToast({ title: '活动不存在', icon: 'none' })
      return
    }

    let canSignup = false
    let btnText = '查看即可'
    if (activity.isPast) {
      btnText = '查看赛果'
    } else if (activity.signed) {
      btnText = '已报名参赛'
    } else if (activity.enrollStatus === 'full') {
      btnText = '名额已满'
    } else if (activity.enrollStatus === 'closed') {
      btnText = '报名已截止'
    } else {
      canSignup = true
      btnText = activity.fee > 0 ? `报名参赛 · ${activity.feeText}` : '免费报名参赛'
    }

    this.setData({
      activity,
      studentName: (student && student.studentName) || '学员',
      canSignup,
      btnText
    })
  },

  onSignup() {
    if (!this.data.canSignup) return
    const session = auth.getSession()
    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)
    if (!student) {
      wx.showToast({ title: '请先选择学员', icon: 'none' })
      return
    }

    wx.showModal({
      title: '确认报名参赛',
      content: `为「${student.studentName}」报名「${this.data.activity.title}」？`,
      success: async (res) => {
        if (!res.confirm) return
        wx.showLoading({ title: '提交中', mask: true })
        try {
          let result = null
          if (api.isRemoteSession()) {
            result = await bridge.signupActivityRemote({
              activityId: this.data.id,
              studentId: student.id
            })
          }
          if (!result) {
            result = activitiesService.signup({
              activityId: this.data.id,
              studentId: student.id,
              studentName: student.studentName,
              parentPhone: session.user.phone,
              orgId: student.orgId
            })
          }
          if (!result.ok) {
            wx.showToast({ title: result.message || '报名失败', icon: 'none' })
            return
          }
          wx.showToast({ title: '报名成功，准时参赛', icon: 'success' })
          this.refresh()
        } catch (err) {
          wx.showToast({ title: (err && err.message) || '报名失败', icon: 'none' })
        } finally {
          wx.hideLoading()
        }
      }
    })
  },

  onCancel() {
    const activity = this.data.activity
    if (!activity || !activity.signed || !activity.signupId) return
    wx.showModal({
      title: '取消报名',
      content: '确定取消这场活动的报名吗？',
      success: async (res) => {
        if (!res.confirm) return
        wx.showLoading({ title: '处理中', mask: true })
        try {
          let result = null
          if (api.isRemoteSession()) {
            result = await bridge.cancelSignupRemote(activity.signupId)
          }
          if (!result) {
            result = activitiesService.cancelSignup(activity.signupId, auth.getSession().user.phone)
          }
          if (!result.ok) {
            wx.showToast({ title: result.message || '取消失败', icon: 'none' })
            return
          }
          wx.showToast({ title: '已取消', icon: 'success' })
          this.refresh()
        } catch (err) {
          wx.showToast({ title: (err && err.message) || '取消失败', icon: 'none' })
        } finally {
          wx.hideLoading()
        }
      }
    })
  }
})
