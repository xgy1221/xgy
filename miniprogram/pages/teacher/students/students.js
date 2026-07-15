const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')

Page({
  data: { list: [] },

  onShow() {
    if (!auth.requireAuth()) return
    // 演示：老师可见全部学员档案；后续可按班级过滤
    const list = studentsService.getAllStudents().map((s) => {
      const enrolls = enrollmentsService.getEnrollmentsDetailedByStudent(s.id)
      return {
        ...s,
        enrollCount: enrolls.length,
        enrollText: enrolls.length
          ? enrolls.map((e) => e.packageName).join('、')
          : '尚未绑定教案'
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
