const auth = require('../../../utils/auth')
const lessonsService = require('../../../services/lessons')

const STATUS_TEXT = {
  upcoming: '未开始',
  ongoing: '进行中',
  finished: '已结束'
}

Page({
  data: {
    id: '',
    lesson: { attendees: [] },
    statusText: '',
    rateVisible: false,
    rateStudentId: '',
    rateName: '',
    rateScore: 5,
    rateComment: '',
    starList: [1, 2, 3, 4, 5]
  },

  onLoad(query) {
    if (!auth.requireAuth()) return
    this.setData({ id: query.id || '' })
  },

  onShow() {
    if (!auth.requireAuth()) return
    this.refresh()
  },

  refresh() {
    const lesson = lessonsService.getLessonById(this.data.id)
    if (!lesson) {
      wx.showToast({ title: '课次不存在', icon: 'none' })
      return
    }
    this.setData({
      lesson,
      statusText: STATUS_TEXT[lesson.status] || lesson.status
    })
  },

  goAddTemp() {
    wx.navigateTo({
      url: `/pages/teacher/add-temp/add-temp?lessonId=${this.data.id}`
    })
  },

  onToggleAbsent(e) {
    const { id, absent } = e.currentTarget.dataset
    lessonsService.markAbsent(this.data.id, id, !!absent)
    this.refresh()
  },

  onFinish() {
    wx.showModal({
      title: '确认下课？',
      content: '下课后请评价学生以消课。学生评价老师可选。临时插班不改变原班归属。',
      success: (res) => {
        if (!res.confirm) return
        const result = lessonsService.finishLesson(this.data.id)
        wx.showToast({ title: result.ok ? '已下课' : result.message, icon: 'none' })
        this.refresh()
      }
    })
  },

  openRate(e) {
    this.setData({
      rateVisible: true,
      rateStudentId: e.currentTarget.dataset.id,
      rateName: e.currentTarget.dataset.name,
      rateScore: 5,
      rateComment: ''
    })
  },

  closeRate() {
    this.setData({ rateVisible: false })
  },

  onStar(e) {
    this.setData({ rateScore: e.currentTarget.dataset.score })
  },

  onComment(e) {
    this.setData({ rateComment: e.detail.value })
  },

  onSubmitRate() {
    const result = lessonsService.rateByTeacher(
      this.data.id,
      this.data.rateStudentId,
      this.data.rateScore,
      this.data.rateComment
    )
    wx.showToast({ title: result.message || (result.ok ? '已评价' : '失败'), icon: 'none' })
    this.setData({ rateVisible: false })
    this.refresh()
  }
})
