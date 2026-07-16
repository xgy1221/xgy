const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const lessonsService = require('../../../services/lessons')
const bridge = require('../../../services/bridge')
const motion = require('../../../utils/motion')
const { todayKey, formatDisplay } = require('../../../utils/date')

const STATUS_TEXT = {
  upcoming: '未开始',
  ongoing: '进行中',
  finished: '已结束'
}

function whenLabel(date) {
  const today = todayKey()
  if (date === today) return '今日待上'
  const t = new Date(`${today}T00:00:00`)
  const d = new Date(`${date}T00:00:00`)
  const diff = Math.round((d - t) / 86400000)
  if (diff === 1) return '明日待上'
  if (diff > 1 && diff <= 7) return `${diff} 天后`
  return formatDisplay(date)
}

Page({
  data: {
    studentName: '',
    orgName: '',
    orgShortName: '',
    children: [],
    currentStudentId: '',
    selectedDate: '',
    displayDate: '',
    markedDates: [],
    lessons: [],
    nextLesson: null,
    weekAheadCount: 0,
    loading: true,
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
    const children = studentsService.getStudentsByPhone(session.user.phone)
    if (!children.length) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    const multiOrg = new Set(children.map((c) => c.orgId)).size > 1
    const childrenView = children.map((c) => ({
      ...c,
      chipText: multiOrg ? `${c.studentName}·${c.orgShortName || c.orgName}` : c.studentName
    }))
    let currentStudentId = session.currentStudentId
    if (!children.some((c) => c.id === currentStudentId)) {
      currentStudentId = children[0].id
      auth.setCurrentStudentId(currentStudentId)
    }
    const current = children.find((c) => c.id === currentStudentId) || {}
    const selectedDate = this.data.selectedDate || todayKey()
    const orgName = current.orgName || ''
    const orgShortName = current.orgShortName || (orgName ? orgName.slice(0, 1) : '校')
    if (orgName) {
      wx.setNavigationBarTitle({ title: orgName })
    }
    this.setData({
      children: childrenView,
      currentStudentId,
      studentName: current.studentName || '',
      orgName,
      orgShortName,
      selectedDate,
      loading: false
    })
    this.refresh(currentStudentId, selectedDate)
  },

  refresh(studentId, date) {
    const markedDates = lessonsService.getLessonDateMarksForStudent(studentId)
    const lessons = lessonsService.getStudentLessonsByDate(studentId, date).map((l) => {
      const me = (l.attendees || []).find((a) => String(a.studentId) === String(studentId)) || {}
      return {
        ...l,
        statusText: STATUS_TEXT[l.status] || l.status,
        myType: me.type || 'regular',
        homeClassName: me.homeClassName || '',
        needRate: l.status === 'finished' && me && !me.absent && !me.studentRated
      }
    })
    const rawNext = lessonsService.getNextLessonForStudent(studentId)
    const nextLesson = rawNext
      ? {
          ...rawNext,
          statusText: STATUS_TEXT[rawNext.status] || rawNext.status,
          whenLabel: whenLabel(rawNext.date)
        }
      : null
    const weekAheadCount = lessonsService.countUpcomingLessons(studentId, 14)
    this.setData({
      markedDates,
      lessons,
      displayDate: formatDisplay(date),
      nextLesson,
      weekAheadCount
    })
  },

  onSelectDate(e) {
    const date = e.detail.date
    motion.tap('light')
    motion.swap(this, () => {
      this.setData({ selectedDate: date })
      this.refresh(this.data.currentStudentId, date)
    }, 80)
  },

  goNextDay(e) {
    const id = e.currentTarget.dataset.id
    const next = this.data.nextLesson
    motion.tap('light')
    if (next && next.date) {
      motion.swap(this, () => {
        this.setData({ selectedDate: next.date })
        this.refresh(this.data.currentStudentId, next.date)
      }, 80)
      return
    }
    if (id) {
      wx.navigateTo({ url: `/pages/student/lesson-detail/lesson-detail?id=${id}` })
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
      this.setData({
        currentStudentId: id,
        studentName: student.studentName || '',
        orgName,
        orgShortName: student.orgShortName || (orgName ? orgName.slice(0, 1) : '校'),
        selectedDate: todayKey()
      })
      this.refresh(id, todayKey())
      this.setData({ contentReady: true })
    } catch (err) {
      this.setData({ contentReady: true })
      wx.showToast({ title: (err && err.message) || '切换失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    motion.tap('light')
    wx.navigateTo({
      url: `/pages/student/lesson-detail/lesson-detail?id=${id}`
    })
  }
})
