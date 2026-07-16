const { ROLE_META, ROLES } = require('../../utils/constants')
const { DEMO_USERS } = require('../../services/mock')
const auth = require('../../utils/auth')
const bridge = require('../../services/bridge')
const motion = require('../../utils/motion')

Page({
  data: {
    phone: '',
    accounts: [],
    loading: false,
    showDemo: false,
    focusPhone: false
  },

  toggleDemo() {
    motion.tap('light')
    this.setData({ showDemo: !this.data.showDemo })
  },

  onLoad() {
    if (auth.isLoggedIn()) {
      this.redirectBySession(auth.getSession())
      return
    }

    const orgs = require('../../services/orgs')
    const accounts = DEMO_USERS.map((u) => ({
      ...u,
      roleText: [
        u.orgId ? orgs.getOrgShortName(u.orgId) : '跨机构家长',
        u.roles.map((r) => ROLE_META[r].shortName).join('/')
      ].join(' · ')
    }))
    accounts.push({
      phone: '13800000031',
      name: '仅录手机号家长',
      avatarText: '新',
      roleText: '学趣 · 待完善学员'
    })
    this.setData({ accounts })
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  onQuickLogin(e) {
    const phone = e.currentTarget.dataset.phone
    motion.tap('medium')
    this.setData({ phone })
    this.doLogin(phone)
  },

  onLogin() {
    motion.tap('light')
    this.doLogin(this.data.phone)
  },

  async doLogin(phone) {
    if (this.data.loading) return
    if (!phone) {
      this.setData({ focusPhone: true })
      wx.showToast({ title: '请先输入手机号', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    wx.showLoading({ title: '登录中', mask: true })
    try {
      const result = await bridge.login(phone)
      if (!result.ok) {
        wx.showToast({ title: result.message, icon: 'none' })
        return
      }
      motion.tap('medium')
      auth.setSession(result.session)
      this.redirectBySession(result.session)
    } catch (e) {
      wx.showToast({ title: (e && e.message) || '登录失败', icon: 'none' })
    } finally {
      wx.hideLoading()
      this.setData({ loading: false })
    }
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
