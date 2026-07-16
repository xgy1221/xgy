const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')

Page({
  data: {
    list: [],
    studentName: '',
    summaryText: ''
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
    const learning = enrollmentsService.summarizeStudentLearning(studentId)

    this.setData({
      list: learning.enrollments,
      studentName: student ? student.studentName : '学员',
      summaryText: learning.summaryText
    })
  },

  onAddPackages() {
    wx.navigateTo({ url: '/pages/onboarding/onboarding?mode=packages' })
  }
})
