const auth = require('../../../utils/auth')
const { ROLE_META } = require('../../../utils/constants')

Page({
  data: {
    name: '',
    phone: '',
    avatarText: '',
    roleName: '',
    multiRole: false
  },
  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    const role = auth.getCurrentRole()
    const u = session.user
    this.setData({
      name: u.name,
      phone: u.phone,
      avatarText: u.avatarText || u.name.slice(0, 1),
      roleName: (ROLE_META[role] && ROLE_META[role].name) || role,
      multiRole: (u.roles || []).length > 1
    })
  },
  onSwitchRole() {
    wx.navigateTo({ url: '/pages/role-select/role-select' })
  },
  onLogout() {
    auth.clearSession()
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
