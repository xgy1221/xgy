const { ROLE_META, ROLES } = require('../../utils/constants')
const { DEMO_USERS, loginByPhone } = require('../../services/mock')
const auth = require('../../utils/auth')

Page({
  data: {
    phone: '',
    accounts: []
  },

  onLoad() {
    if (auth.isLoggedIn()) {
      this.redirectBySession(auth.getSession())
      return
    }

    const accounts = DEMO_USERS.map((u) => ({
      ...u,
      roleText: u.roles.map((r) => ROLE_META[r].shortName).join(' / ')
    }))
    accounts.push({
      phone: '13800000031',
      name: '仅录手机号家长',
      avatarText: '新',
      roleText: '待完善学员'
    })
    this.setData({ accounts })
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  onQuickLogin(e) {
    const phone = e.currentTarget.dataset.phone
    this.setData({ phone })
    this.doLogin(phone)
  },

  onLogin() {
    this.doLogin(this.data.phone)
  },

  doLogin(phone) {
    const result = loginByPhone(phone)
    if (!result.ok) {
      wx.showToast({ title: result.message, icon: 'none' })
      return
    }

    auth.setSession(result.session)
    this.redirectBySession(result.session)
  },

  redirectBySession(session) {
    if (!session.currentRole) {
      wx.reLaunch({ url: '/pages/role-select/role-select' })
      return
    }
    if (session.currentRole === ROLES.STUDENT && session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    auth.switchToRoleHome(session.currentRole)
  }
})
