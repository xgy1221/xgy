const auth = require('../../../utils/auth')
const { ROLE_META } = require('../../../utils/constants')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')
const lessonsService = require('../../../services/lessons')
const bridge = require('../../../services/bridge')
const api = require('../../../services/api')
const motion = require('../../../utils/motion')
const { formatDisplay } = require('../../../utils/date')

const PAGE_SIZE = 8
const LAST_PKG_KEY = 'xgy_last_package'

Page({
  data: {
    name: '',
    phone: '',
    avatarText: '',
    roleName: '',
    orgName: '',
    multiRole: false,
    roleOptions: [],
    currentRoleKey: '',
    showAccount: false,
    children: [],
    currentStudentId: '',
    packages: [],
    currentPackageId: '',
    currentPackage: null,
    lessonRows: [],
    displayLessons: [],
    hasMore: false,
    shownCount: 0,
    evalCount: 0,
    latestEval: null,
    contentReady: true
  },

  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    if (session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    lessonsService.ensureLessons()

    const role = auth.getCurrentRole()
    const u = session.user
    const children = studentsService.getStudentsByPhone(u.phone).map((c) => ({
      ...c,
      chipText: c.orgShortName ? `${c.studentName}·${c.orgShortName}` : c.studentName
    }))
    let currentStudentId = session.currentStudentId
    if (children.length && !children.some((c) => c.id === currentStudentId)) {
      currentStudentId = children[0].id
      auth.setCurrentStudentId(currentStudentId)
    }
    const current = children.find((c) => c.id === currentStudentId) || {}
    const orgName = current.orgName || ''
    if (orgName) wx.setNavigationBarTitle({ title: orgName })
    const roleOptions = (u.roles || []).map((key) => ROLE_META[key]).filter(Boolean)

    this.setData({
      name: u.name,
      phone: u.phone,
      avatarText: u.avatarText || u.name.slice(0, 1),
      roleName: (ROLE_META[role] && ROLE_META[role].name) || role,
      orgName,
      multiRole: roleOptions.length > 1,
      roleOptions,
      currentRoleKey: role,
      children,
      currentStudentId
    })
    this.loadPackagesForStudent(currentStudentId)
  },

  toggleAccount() {
    motion.tap('light')
    this.setData({ showAccount: !this.data.showAccount })
  },

  getLastPackageMap() {
    return wx.getStorageSync(LAST_PKG_KEY) || {}
  },

  setLastPackage(studentId, packageId) {
    if (!studentId || !packageId) return
    const map = this.getLastPackageMap()
    map[studentId] = packageId
    wx.setStorageSync(LAST_PKG_KEY, map)
  },

  loadPackagesForStudent(studentId) {
    if (!studentId) {
      this.setData({
        packages: [],
        currentPackageId: '',
        currentPackage: null,
        lessonRows: [],
        displayLessons: [],
        hasMore: false,
        shownCount: 0,
        evalCount: 0
      })
      return
    }
    const learning = enrollmentsService.summarizeStudentLearning(studentId)
    const packages = learning.enrollments.map((e) => ({
      id: e.packageId,
      enrollmentId: e.id,
      name: e.packageName,
      subject: e.subject,
      status: e.status,
      usedLessons: e.usedLessons,
      totalLessons: e.totalLessons,
      remainLessons: e.remainLessons,
      progress: e.progress,
      chipText: e.packageName.length > 8 ? `${e.packageName.slice(0, 8)}…` : e.packageName
    }))

    const lastMap = this.getLastPackageMap()
    let currentPackageId = lastMap[studentId] || ''
    if (!packages.some((p) => p.id === currentPackageId)) {
      currentPackageId = packages[0] ? packages[0].id : ''
    }
    if (currentPackageId) this.setLastPackage(studentId, currentPackageId)

    this.setData({ packages, currentPackageId })
    return this.loadLessonsForPackage(studentId, currentPackageId, packages)
  },

  async loadLessonsForPackage(studentId, packageId, packages) {
    const pkgList = packages || this.data.packages
    const currentPackage = pkgList.find((p) => p.id === packageId) || null
    if (!studentId || !packageId) {
      this.setData({
        currentPackage,
        lessonRows: [],
        displayLessons: [],
        hasMore: false,
        shownCount: 0,
        evalCount: 0,
        latestEval: null
      })
      return
    }

    let lessonRows = []
    if (api.isRemoteSession()) {
      try {
        const remote = await bridge.fetchPackageLessonsRemote(studentId, packageId)
        if (remote) {
          lessonRows = remote.map((l) => ({
            ...l,
            dateText: formatDisplay(l.date)
          }))
        }
      } catch (e) {
        lessonRows = []
      }
    }
    if (!lessonRows.length) {
      lessonRows = lessonsService.getStudentLessonsByPackage(studentId, packageId).map((l) => ({
        ...l,
        dateText: formatDisplay(l.date)
      }))
    }

    const evalCount = lessonRows.filter((l) => l.hasTeacherEval).length
    const latestEval = lessonRows.find((l) => l.hasTeacherEval) || null
    const shownCount = Math.min(PAGE_SIZE, lessonRows.length)

    this.setData({
      currentPackage,
      lessonRows,
      displayLessons: lessonRows.slice(0, shownCount),
      shownCount,
      hasMore: lessonRows.length > shownCount,
      evalCount,
      latestEval
    })
  },

  async onPickRole(e) {
    const role = e.currentTarget.dataset.role
    if (!role || role === this.data.currentRoleKey) return
    wx.showLoading({ title: '切换中', mask: true })
    try {
      await bridge.remoteSwitchRole(role)
      wx.reLaunch({ url: auth.getRoleHome(role) })
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '切换失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  async onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id || id === this.data.currentStudentId) return
    motion.tap('medium')
    this.setData({ contentReady: false })
    wx.showLoading({ title: '切换中', mask: true })
    try {
      await bridge.remoteSwitchStudent(id)
      const student = this.data.children.find((c) => c.id === id) || {}
      const orgName = student.orgName || ''
      if (orgName) wx.setNavigationBarTitle({ title: orgName })
      this.setData({ currentStudentId: id, orgName })
      await this.loadPackagesForStudent(id)
      this.setData({ contentReady: true })
    } catch (err) {
      this.setData({ contentReady: true })
      wx.showToast({ title: (err && err.message) || '切换失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  onSwitchPackage(e) {
    const id = e.currentTarget.dataset.id
    if (!id || id === this.data.currentPackageId) return
    motion.tap('light')
    motion.swap(this, () => {
      this.setLastPackage(this.data.currentStudentId, id)
      this.setData({ currentPackageId: id })
      this.loadLessonsForPackage(this.data.currentStudentId, id)
    }, 90)
  },

  onLoadMore() {
    const { lessonRows, shownCount } = this.data
    const next = Math.min(shownCount + PAGE_SIZE, lessonRows.length)
    this.setData({
      shownCount: next,
      displayLessons: lessonRows.slice(0, next),
      hasMore: lessonRows.length > next
    })
  },

  goLesson(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/student/lesson-detail/lesson-detail?id=${id}` })
  },

  onAddChild() {
    wx.navigateTo({ url: '/pages/onboarding/onboarding' })
  },

  goAddPackages() {
    wx.navigateTo({ url: '/pages/onboarding/onboarding?mode=packages' })
  },

  goSchedule() {
    wx.navigateTo({ url: '/pages/student/schedule/schedule' })
  },

  goScheduleTab() {
    wx.reLaunch({ url: '/pages/student/home/home' })
  },

  goActivities() {
    wx.redirectTo({ url: '/pages/student/activities/activities' })
  },

  async onLogout() {
    await bridge.remoteLogout()
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
