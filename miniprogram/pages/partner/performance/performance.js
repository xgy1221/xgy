const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
Page({
  data: { stats: {} },
  onShow() {
    if (!auth.requireAuth()) return
    this.setData({ stats: mock.PARTNER_STATS })
  }
})
