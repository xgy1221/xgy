const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
Page({
  data: { userCount: 0, orgCount: 0 },
  onShow() {
    if (!auth.requireAuth()) return
    this.setData({
      userCount: mock.getAdminUserList().length,
      orgCount: mock.ORG_NODES.length
    })
  },
  go(e) {
    wx.redirectTo({ url: e.currentTarget.dataset.url })
  }
})
