const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')

Page({
  data: { list: [] },
  onShow() {
    if (!auth.requireAuth()) return
    const list = mock.getAdminUserList().map((u) => ({
      ...u,
      rolesText: (u.roles || []).join(' / ')
    }))
    this.setData({ list })
  }
})
