const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const activitiesService = require('../../../services/activities')

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

  refresh() {
    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)
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
      success: (res) => {
        if (!res.confirm) return
        const result = activitiesService.signup({
          activityId: this.data.id,
          studentId: student.id,
          studentName: student.studentName,
          parentPhone: session.user.phone,
          orgId: student.orgId
        })
        if (!result.ok) {
          wx.showToast({ title: result.message, icon: 'none' })
          return
        }
        wx.showToast({ title: '报名成功，准时参赛', icon: 'success' })
        this.refresh()
      }
    })
  },

  onCancel() {
    const activity = this.data.activity
    if (!activity || !activity.signed || !activity.signupId) return
    wx.showModal({
      title: '取消报名',
      content: '确定取消这场活动的报名吗？',
      success: (res) => {
        if (!res.confirm) return
        const result = activitiesService.cancelSignup(
          activity.signupId,
          auth.getSession().user.phone
        )
        if (!result.ok) {
          wx.showToast({ title: result.message, icon: 'none' })
          return
        }
        wx.showToast({ title: '已取消', icon: 'success' })
        this.refresh()
      }
    })
  }
})
