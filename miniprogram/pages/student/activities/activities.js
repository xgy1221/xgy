const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const activitiesService = require('../../../services/activities')
const bridge = require('../../../services/bridge')
const api = require('../../../services/api')

Page({
  data: {
    tab: 'open',
    children: [],
    currentStudentId: '',
    studentName: '',
    orgName: '',
    openList: [],
    pastList: [],
    myList: [],
    openCount: 0,
    pastCount: 0,
    myCount: 0
  },

  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    if (session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    this.refresh()
  },

  async refresh() {
    const session = auth.getSession()
    const children = studentsService.getStudentsByPhone(session.user.phone).map((c) => ({
      ...c,
      chipText: c.orgShortName ? `${c.studentName}·${c.orgShortName}` : c.studentName
    }))
    let currentStudentId = session.currentStudentId
    if (children.length && !children.some((c) => c.id === currentStudentId)) {
      currentStudentId = children[0].id
      auth.setCurrentStudentId(currentStudentId)
    }

    if (api.isRemoteSession() && currentStudentId) {
      try {
        await bridge.refreshActivitiesForCurrentStudent()
      } catch (e) {
        // 本地缓存仍可用
      }
    }

    const student = studentsService.getStudentById(currentStudentId)
    const orgId = (student && student.orgId) || auth.getCurrentOrgId()
    const phone = session.user.phone

    const openList = activitiesService.listOpen(orgId, currentStudentId)
    const pastList = activitiesService.listPast(orgId, currentStudentId)
    const myList = activitiesService.listMySignups(phone, currentStudentId)

    this.setData({
      children,
      currentStudentId,
      studentName: (student && student.studentName) || '学员',
      orgName: (student && student.orgName) || '',
      openList,
      pastList,
      myList,
      openCount: openList.length,
      pastCount: pastList.length,
      myCount: myList.length
    })
  },

  async onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id || id === this.data.currentStudentId) return
    wx.showLoading({ title: '切换中', mask: true })
    try {
      await bridge.remoteSwitchStudent(id)
      await this.refresh()
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '切换失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  onTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (!tab || tab === this.data.tab) return
    this.setData({ tab })
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/student/activity-detail/activity-detail?id=${id}` })
  }
})
