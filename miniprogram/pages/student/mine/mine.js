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
    roleOptions: [],
    currentRoleKey: '',
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
    const roleOptions = (u.roles || []).map((key) => ROLE_META[key]).filter(Boolean)

    this.setData({
      name: u.name,
      phone: u.phone,
      avatarText: u.avatarText || u.name.slice(0, 1),
      roleName: (ROLE_META[role] && ROLE_META[role].name) || role,
      multiRole: roleOptions.length > 1,
      roleOptions,
      currentRoleKey: role,
      children,
      currentStudentId
    })
  },

  onPickRole(e) {
    const role = e.currentTarget.dataset.role
    if (!role || role === this.data.currentRoleKey) return
    auth.switchToRoleHome(role)
  },

  onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    auth.setCurrentStudentId(id)
    this.setData({ currentStudentId: id })
    wx.showToast({ title: '已切换学员', icon: 'success' })
  },

  onAddChild() {
    wx.navigateTo({ url: '/pages/onboarding/onboarding' })
  },

  goCourses() {
    wx.navigateTo({ url: '/pages/student/courses/courses' })
  },

  goActivities() {
    wx.redirectTo({ url: '/pages/student/activities/activities' })
  },

  onLogout() {
    auth.clearSession()
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
