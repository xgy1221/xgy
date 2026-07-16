const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
Page({
  data: { list: [], orgName: '' },
  onShow() {
    if (!auth.requireAuth()) return
    const orgId = auth.getCurrentOrgId()
    const orgs = require('../../../services/orgs')
    this.setData({
      orgName: orgs.getOrgName(orgId) || '当前机构',
      list: mock.getOrgNodes(orgId)
    })
  }
})
