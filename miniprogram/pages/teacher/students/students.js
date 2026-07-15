const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')

Page({
  data: { list: [] },

  onShow() {
    if (!auth.requireAuth()) return
    const orgId = auth.getCurrentOrgId()
    const list = studentsService.listStudentsByOrg(orgId).map((s) => {
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
