const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')

Page({
  data: {
    name: '',
    campus: '',
    shareRatio: '-',
    stats: {}
  },
  onShow() {
    if (!auth.requireAuth()) return
    const u = auth.getSession().user
    const stats = mock.PARTNER_STATS
    this.setData({
      name: u.name,
      campus: stats.campus || u.campus || '本校区',
      shareRatio: u.shareRatio || stats.shareRatio || '-',
      stats
    })
  }
})
