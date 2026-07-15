const auth = require('../../../utils/auth')
const packagesService = require('../../../services/packages')

Page({
  data: { list: [] },

  onShow() {
    if (!auth.requireAuth()) return
    this.refresh()
  },

  refresh() {
    const list = packagesService.getAllPackages().map((p) => ({
      ...p,
      outlineText: (p.outline || []).join(' / ')
    }))
    this.setData({ list })
  },

  onToggle(e) {
    packagesService.togglePackageStatus(e.currentTarget.dataset.id)
    this.refresh()
  }
})
