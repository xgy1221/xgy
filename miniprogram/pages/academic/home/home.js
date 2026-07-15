const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')

Page({
  data: { campus: '', pendingSchedule: 0, pendingPay: 0, enrolls: [] },
  onShow() {
    if (!auth.requireAuth()) return
    const u = auth.getSession().user
    const enrolls = mock.ACADEMIC_ENROLLS
    this.setData({
      campus: u.campus || '校区',
      pendingSchedule: enrolls.filter((e) => e.status === '待排课').length,
      pendingPay: enrolls.filter((e) => e.status === '待缴费').length,
      enrolls
    })
  },
  goImport() {
    wx.navigateTo({ url: '/pages/academic/import/import' })
  }
})
