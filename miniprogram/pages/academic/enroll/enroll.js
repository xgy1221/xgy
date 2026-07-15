const auth = require('../../../utils/auth')
const enrollmentsService = require('../../../services/enrollments')
const studentsService = require('../../../services/students')

Page({
  data: { list: [] },

  onShow() {
    if (!auth.requireAuth()) return
    const orgId = auth.getCurrentOrgId()
    const list = enrollmentsService.getAllDetailedEnrollments(orgId).map((e) => {
      const stu = studentsService.getStudentById(e.studentId) || {}
      return {
        ...e,
        studentName: stu.studentName || '未知学员',
        parentPhone: stu.parentPhone || '-'
      }
    })
    this.setData({ list })
  },

  goSetup() {
    wx.navigateTo({ url: '/pages/academic/setup/setup' })
  },

  goEdit(e) {
    const { phone, id } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/academic/setup/setup?phone=${phone || ''}&studentId=${id || ''}`
    })
  }
})
