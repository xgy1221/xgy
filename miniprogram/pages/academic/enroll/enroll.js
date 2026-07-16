const auth = require('../../../utils/auth')
const enrollmentsService = require('../../../services/enrollments')
const studentsService = require('../../../services/students')
const api = require('../../../services/api')

Page({
  data: { list: [] },

  onShow() {
    if (!auth.requireAuth()) return
    this.refresh()
  },

  async refresh() {
    try {
      if (api.isRemoteSession()) {
        const rows = await api.fetchEnrollments()
        const list = (rows || []).map((e) => ({
          id: e.id,
          studentId: e.studentId,
          studentName: e.studentName || '未知学员',
          parentPhone: e.parentPhone || '-',
          packageName: e.packageName || '-',
          remainLessons: e.remainLessons,
          totalLessons: e.totalLessons,
          status: api.mapEnrollmentStatus(e.status),
          progress: e.progress,
          source: e.source || '教务代录'
        }))
        this.setData({ list })
        return
      }
    } catch (e) {
      wx.showToast({ title: (e && e.message) || '报读加载失败', icon: 'none' })
    }

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
