const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const activitiesService = require('../../../services/activities')

Page({
  data: {
    tab: 'open',
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

  refresh() {
    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)
    const orgId = (student && student.orgId) || auth.getCurrentOrgId()
    const phone = auth.getSession().user.phone

    const openList = activitiesService.listOpen(orgId, studentId)
    const pastList = activitiesService.listPast(orgId, studentId)
    const myList = activitiesService.listMySignups(phone, studentId)

    this.setData({
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
