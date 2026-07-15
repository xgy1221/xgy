const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')

Page({
  data: {
    list: [],
    studentName: ''
  },

  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    if (session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }

    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)
    const list = enrollmentsService.getEnrollmentsDetailedByStudent(studentId).map((e) => {
      const used = Math.max(0, e.totalLessons - e.remainLessons)
      const progress = e.totalLessons ? Math.round((used / e.totalLessons) * 100) : 0
      return { ...e, progress }
    })

    this.setData({
      list,
      studentName: student ? student.studentName : '学员'
    })
  },

  onAddPackages() {
    wx.navigateTo({ url: '/pages/onboarding/onboarding?mode=packages' })
  }
})
