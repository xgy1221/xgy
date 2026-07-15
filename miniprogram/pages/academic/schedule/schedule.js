const auth = require('../../../utils/auth')
const lessonsService = require('../../../services/lessons')
const { todayKey, formatDisplay } = require('../../../utils/date')

const STATUS_TEXT = {
  upcoming: '未开始',
  ongoing: '进行中',
  finished: '已结束'
}

Page({
  data: {
    selectedDate: '',
    displayDate: '',
    markedDates: [],
    groups: []
  },

  onShow() {
    if (!auth.requireAuth()) return
    lessonsService.ensureLessons()
    const selectedDate = this.data.selectedDate || todayKey()
    this.setData({ selectedDate })
    this.refresh(selectedDate)
  },

  refresh(date) {
    const markedDates = lessonsService.getLessonDateMarksForTeacher()
    const lessons = lessonsService.getTeacherLessonsByDate(date).map((l) => {
      const present = l.attendees.filter((a) => !a.absent)
      const ratedCount = present.filter((a) => a.teacherRated || a.studentRated).length
      return {
        ...l,
        statusText: STATUS_TEXT[l.status] || l.status,
        presentCount: present.length,
        ratedCount
      }
    })

    const map = {}
    lessons.forEach((l) => {
      const key = l.teacherName || '未分配老师'
      if (!map[key]) {
        map[key] = {
          teacherName: key,
          avatarText: key.slice(0, 1),
          lessons: []
        }
      }
      map[key].lessons.push(l)
    })

    const groups = Object.keys(map)
      .map((k) => map[k])
      .map((g) => ({
        ...g,
        lessons: g.lessons.sort((a, b) => a.startTime.localeCompare(b.startTime))
      }))

    this.setData({
      markedDates,
      groups,
      displayDate: formatDisplay(date)
    })
  },

  onSelectDate(e) {
    const date = e.detail.date
    this.setData({ selectedDate: date })
    this.refresh(date)
  },

  goDetail(e) {
    wx.navigateTo({
      url: `/pages/academic/lesson-view/lesson-view?id=${e.currentTarget.dataset.id}`
    })
  }
})
