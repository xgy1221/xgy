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
    lessons: []
  },

  onShow() {
    if (!auth.requireAuth()) return
    lessonsService.ensureLessons()
    const selectedDate = this.data.selectedDate || todayKey()
    this.setData({ selectedDate })
    this.refresh(selectedDate)
  },

  refresh(date) {
    const orgId = auth.getCurrentOrgId()
    const markedDates = lessonsService.getLessonDateMarksForTeacher(orgId)
    const lessons = lessonsService.getTeacherLessonsByDate(date, orgId).map((l) => ({
      ...l,
      statusText: STATUS_TEXT[l.status] || l.status
    }))
    this.setData({
      markedDates,
      lessons,
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
      url: `/pages/teacher/lesson-detail/lesson-detail?id=${e.currentTarget.dataset.id}`
    })
  },

  goAddTemp(e) {
    wx.navigateTo({
      url: `/pages/teacher/add-temp/add-temp?lessonId=${e.currentTarget.dataset.id}`
    })
  }
})

