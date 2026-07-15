const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')

Page({
  data: { list: [] },
  onShow() {
    if (!auth.requireAuth()) return
    const orgId = auth.getCurrentOrgId()
    const list = mock.getAdminUserList(orgId).map((u) => ({
      ...u,
      rolesText: (u.roles || []).join(' / ')
    }))
    this.setData({ list })
  }
})
