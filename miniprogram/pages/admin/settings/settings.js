const auth = require('../../../utils/auth')
Page({
  data: {
    settings: { selfBook: true, hourAlert: true, partnerRefund: false }
  },
  onShow() {
    if (!auth.requireAuth()) return
  },
  onToggle(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ [`settings.${key}`]: e.detail.value })
  }
})
