const { ROLE_META, ROLES } = require('../../utils/constants')
const auth = require('../../utils/auth')

Page({
  data: {
    userName: '',
    roles: [],
    lastRole: ''
  },

  onShow() {
    const session = auth.getSession()
    if (!session) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }

    const roles = (session.user.roles || []).map((key) => ROLE_META[key]).filter(Boolean)
    const lastRole = auth.getLastRole(session.user.phone) || ''
    this.setData({
      userName: session.user.name,
      roles,
      lastRole
    })
  },

  onSelect(e) {
    const role = e.currentTarget.dataset.role
    auth.setCurrentRole(role)
    const session = auth.getSession()
    if (role === ROLES.STUDENT && session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    auth.switchToRoleHome(role)
  }
})
