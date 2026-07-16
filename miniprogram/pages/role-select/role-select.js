const { ROLE_META, ROLES } = require('../../utils/constants')
const auth = require('../../utils/auth')
const bridge = require('../../services/bridge')

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

  async onSelect(e) {
    const role = e.currentTarget.dataset.role
    if (!role) return
    wx.showLoading({ title: '切换中', mask: true })
    try {
      const session = await bridge.remoteSwitchRole(role)
      if (role === ROLES.STUDENT && session.needsOnboarding) {
        wx.reLaunch({ url: '/pages/onboarding/onboarding' })
        return
      }
      wx.reLaunch({ url: auth.getRoleHome(role) })
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '切换失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  }
})
