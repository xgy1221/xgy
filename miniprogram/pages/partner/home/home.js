const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
const api = require('../../../services/api')

Page({
  data: {
    name: '',
    campus: '',
    shareRatio: '-',
    stats: {}
  },
  async onShow() {
    if (!auth.requireAuth()) return
    const u = auth.getSession().user
    let stats = { ...mock.PARTNER_STATS }
    let shareRatio = u.shareRatio || stats.shareRatio || '15%'
    try {
      if (api.isRemoteSession()) {
        const summary = await api.fetchFinanceSummary()
        const ratio = summary.shareRatio != null ? Number(summary.shareRatio) : 0.15
        const share =
          summary.shareAmount != null
            ? Number(summary.shareAmount)
            : Math.round((Number(summary.netAmount) || 0) * ratio)
        shareRatio = `${Math.round(ratio * 100)}%`
        stats = {
          campus: (summary.byCampus && summary.byCampus[0] && summary.byCampus[0].campus) || '本校区',
          monthReceived: Number(summary.paidAmount) || 0,
          shareAmount: share,
          monthNewStudents: '-',
          monthRefund: Number(summary.refundAmount) || 0,
          pendingOrders: Number(summary.pendingAmount) > 0 ? summary.orderCount || 0 : 0,
          monthSales: Number(summary.signedAmount) || 0
        }
      }
    } catch (e) {
      // 回退本地演示
    }
    this.setData({
      name: u.name,
      campus: stats.campus || u.campus || '本校区',
      shareRatio,
      stats
    })
  },
  goPerf() {
    wx.navigateTo({ url: '/pages/partner/performance/performance' })
  },
  goTeam() {
    wx.navigateTo({ url: '/pages/partner/team/team' })
  }
})
