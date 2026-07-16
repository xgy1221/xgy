const auth = require('../../../utils/auth')
const { ROLE_META } = require('../../../utils/constants')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')
const lessonsService = require('../../../services/lessons')
const { formatDisplay } = require('../../../utils/date')

const PAGE_SIZE = 8
const LAST_PKG_KEY = 'xgy_last_package'

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
    packages: [],
    currentPackageId: '',
    currentPackage: null,
    lessonRows: [],
    displayLessons: [],
    hasMore: false,
    shownCount: 0,
    evalCount: 0
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
    this.loadPackagesForStudent(currentStudentId)
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
    this.loadLessonsForPackage(studentId, currentPackageId, packages)
  },

  loadLessonsForPackage(studentId, packageId, packages) {
    const pkgList = packages || this.data.packages
    const currentPackage = pkgList.find((p) => p.id === packageId) || null
    if (!studentId || !packageId) {
      this.setData({
        currentPackage,
        lessonRows: [],
        displayLessons: [],
        hasMore: false,
        shownCount: 0,
        evalCount: 0
      })
      return
    }

    const lessonRows = lessonsService.getStudentLessonsByPackage(studentId, packageId).map((l) => ({
      ...l,
      dateText: formatDisplay(l.date)
    }))
    const evalCount = lessonRows.filter((l) => l.hasTeacherEval).length
    const shownCount = Math.min(PAGE_SIZE, lessonRows.length)

    this.setData({
      currentPackage,
      lessonRows,
      displayLessons: lessonRows.slice(0, shownCount),
      shownCount,
      hasMore: lessonRows.length > shownCount,
      evalCount
    })
  },

  onPickRole(e) {
    const role = e.currentTarget.dataset.role
    if (!role || role === this.data.currentRoleKey) return
    auth.switchToRoleHome(role)
  },

  onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id || id === this.data.currentStudentId) return
    auth.setCurrentStudentId(id)
    this.setData({ currentStudentId: id })
    this.loadPackagesForStudent(id)
  },

  onSwitchPackage(e) {
    const id = e.currentTarget.dataset.id
    if (!id || id === this.data.currentPackageId) return
    this.setLastPackage(this.data.currentStudentId, id)
    this.setData({ currentPackageId: id })
    this.loadLessonsForPackage(this.data.currentStudentId, id)
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

  goActivities() {
    wx.redirectTo({ url: '/pages/student/activities/activities' })
  },

  onLogout() {
    auth.clearSession()
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
