const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
Page({
  data: { name:'', shareRatio:'-', stats: {} },
  onShow() {
    if (!auth.requireAuth()) return
    const u = auth.getSession().user
    this.setData({
      name: u.name,
      shareRatio: u.shareRatio || '-',
      stats: mock.PARTNER_STATS
    })
  }
})
