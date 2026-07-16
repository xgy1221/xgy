const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
Page({
  data: { list: [] },
  onShow() {
    if (!auth.requireAuth()) return
    this.setData({ list: mock.PARTNER_TEAM })
  }
})
