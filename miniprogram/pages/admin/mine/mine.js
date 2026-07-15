const auth = require('../../../utils/auth')
const { ROLE_META } = require('../../../utils/constants')

Page({
  data: {
    name: '',
    phone: '',
    avatarText: '',
    roleName: '',
    multiRole: false,
    roleOptions: [],
    currentRoleKey: ''
  },
  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    const role = auth.getCurrentRole()
    const u = session.user
    const roleOptions = (u.roles || []).map((key) => ROLE_META[key]).filter(Boolean)
    this.setData({
      name: u.name,
      phone: u.phone,
      avatarText: u.avatarText || u.name.slice(0, 1),
      roleName: (ROLE_META[role] && ROLE_META[role].name) || role,
      multiRole: roleOptions.length > 1,
      roleOptions,
      currentRoleKey: role
    })
  },
  onPickRole(e) {
    const role = e.currentTarget.dataset.role
    if (!role || role === this.data.currentRoleKey) return
    auth.switchToRoleHome(role)
  },
  onLogout() {
    auth.clearSession()
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
