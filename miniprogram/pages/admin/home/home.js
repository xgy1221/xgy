const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')

Page({
  data: { stats: {} },
  onShow() {
    if (!auth.requireAuth()) return
    this.setData({ stats: mock.ADMIN_STATS })
  },
  go(e) {
    wx.redirectTo({ url: e.currentTarget.dataset.url })
  }
})
