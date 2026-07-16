const auth = require('../../../utils/auth')
const { ROLE_META } = require('../../../utils/constants')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')

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
    currentStudentId: '',
    familyLearning: [],
    showAllKids: true
  },

  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    if (session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    const role = auth.getCurrentRole()
    const u = session.user
    const children = studentsService.getStudentsByPhone(u.phone)
    let currentStudentId = session.currentStudentId
    if (children.length && !children.some((c) => c.id === currentStudentId)) {
      currentStudentId = children[0].id
      auth.setCurrentStudentId(currentStudentId)
    }
    const roleOptions = (u.roles || []).map((key) => ROLE_META[key]).filter(Boolean)
    const familyLearning = children.map((c) => {
      const learning = enrollmentsService.summarizeStudentLearning(c.id)
      return {
        ...c,
        isCurrent: c.id === currentStudentId,
        packageCount: learning.packageCount,
        usedLessons: learning.usedLessons,
        totalLessons: learning.totalLessons,
        remainLessons: learning.remainLessons,
        summaryText: learning.summaryText,
        enrollments: learning.enrollments,
        expanded: true
      }
    })

    this.setData({
      name: u.name,
      phone: u.phone,
      avatarText: u.avatarText || u.name.slice(0, 1),
      roleName: (ROLE_META[role] && ROLE_META[role].name) || role,
      multiRole: roleOptions.length > 1,
      roleOptions,
      currentRoleKey: role,
      children,
      currentStudentId,
      familyLearning
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
    const familyLearning = (this.data.familyLearning || []).map((c) => ({
      ...c,
      isCurrent: c.id === id
    }))
    this.setData({ currentStudentId: id, familyLearning })
    wx.showToast({ title: '已切换，课表/比赛随当前孩子', icon: 'none' })
  },

  onToggleKid(e) {
    const id = e.currentTarget.dataset.id
    const familyLearning = (this.data.familyLearning || []).map((c) =>
      c.id === id ? { ...c, expanded: !c.expanded } : c
    )
    this.setData({ familyLearning })
  },

  onAddChild() {
    wx.navigateTo({ url: '/pages/onboarding/onboarding' })
  },

  goAddPackages(e) {
    const id = e.currentTarget.dataset.id
    if (id) auth.setCurrentStudentId(id)
    wx.navigateTo({ url: '/pages/onboarding/onboarding?mode=packages' })
  },

  goSchedule() {
    wx.navigateTo({ url: '/pages/student/schedule/schedule' })
  },

  goActivities() {
    wx.redirectTo({ url: '/pages/student/activities/activities' })
  },

  onLogout() {
    auth.clearSession()
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
