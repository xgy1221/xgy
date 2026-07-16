const auth = require('../../../utils/auth')
const lessonsService = require('../../../services/lessons')
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
    selectedDate: '',
    displayDate: '',
    markedDates: [],
    lessons: [],
    nextLesson: null,
    loading: true,
    contentReady: true
  },

  onShow() {
    if (!auth.requireAuth()) return
    lessonsService.ensureLessons()
    const selectedDate = this.data.selectedDate || todayKey()
    this.setData({ selectedDate, loading: false })
    this.refresh(selectedDate)
  },

  refresh(date) {
    const orgId = auth.getCurrentOrgId()
    const markedDates = lessonsService.getLessonDateMarksForTeacher(orgId)
    const lessons = lessonsService.getTeacherLessonsByDate(date, orgId).map((l) => ({
      ...l,
      statusText: STATUS_TEXT[l.status] || l.status,
      attendeeCount: (l.attendees || []).length
    }))

    const today = todayKey()
    const upcoming = lessonsService
      .getAllLessons(orgId)
      .filter((l) => l.status !== 'finished' && l.date >= today)
      .sort((a, b) => `${a.date}${a.startTime}`.localeCompare(`${b.date}${b.startTime}`))
    const raw = upcoming[0] || null
    const nextLesson = raw
      ? {
          ...raw,
          statusText: STATUS_TEXT[raw.status] || raw.status,
          whenLabel: whenLabel(raw.date),
          attendeeCount: (raw.attendees || []).length
        }
      : null

    this.setData({
      markedDates,
      lessons,
      displayDate: formatDisplay(date),
      nextLesson
    })
  },

  onSelectDate(e) {
    const date = e.detail.date
    motion.tap('light')
    motion.swap(this, () => {
      this.setData({ selectedDate: date })
      this.refresh(date)
    }, 80)
  },

  goNextDay() {
    const next = this.data.nextLesson
    if (!next) return
    motion.tap('light')
    motion.swap(this, () => {
      this.setData({ selectedDate: next.date })
      this.refresh(next.date)
    }, 80)
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    motion.tap('light')
    wx.navigateTo({
      url: `/pages/teacher/lesson-detail/lesson-detail?id=${id}`
    })
  },

  goAddTemp(e) {
    const id = e.currentTarget.dataset.id
    motion.tap('medium')
    wx.navigateTo({
      url: `/pages/teacher/add-temp/add-temp?lessonId=${id}`
    })
  }
})
