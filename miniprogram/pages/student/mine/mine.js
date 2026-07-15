const auth = require('../../../utils/auth')
const { ROLE_META } = require('../../../utils/constants')
const studentsService = require('../../../services/students')

Page({
  data: {
    name: '',
    phone: '',
    avatarText: '',
    roleName: '',
    multiRole: false,
    children: [],
    currentStudentId: ''
  },

  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    const role = auth.getCurrentRole()
    const u = session.user
    const children = studentsService.getStudentsByPhone(u.phone)
    let currentStudentId = session.currentStudentId
    if (children.length && !children.some((c) => c.id === currentStudentId)) {
      currentStudentId = children[0].id
      auth.setCurrentStudentId(currentStudentId)
    }

    this.setData({
      name: u.name,
      phone: u.phone,
      avatarText: u.avatarText || u.name.slice(0, 1),
      roleName: (ROLE_META[role] && ROLE_META[role].name) || role,
      multiRole: (u.roles || []).length > 1,
      children,
      currentStudentId
    })
  },

  onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    auth.setCurrentStudentId(id)
    this.setData({ currentStudentId: id })
    wx.showToast({ title: '已切换学员', icon: 'success' })
  },

  onSwitchRole() {
    wx.navigateTo({ url: '/pages/role-select/role-select' })
  },

  onLogout() {
    auth.clearSession()
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
