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
    lesson: {},
    me: {},
    statusText: '',
    canRate: false,
    score: 5,
    comment: '',
    starList: [1, 2, 3, 4, 5]
  },

  onShow() {
    if (!auth.requireAuth()) return
    this.refresh()
  },

  onLoad(query) {
    this.setData({ id: query.id || '' })
  },

  refresh() {
    const lesson = lessonsService.getLessonById(this.data.id)
    if (!lesson) {
      wx.showToast({ title: '课次不存在', icon: 'none' })
      return
    }
    const studentId = auth.getCurrentStudentId()
    const me = lesson.attendees.find((a) => a.studentId === studentId) || {}
    const canRate = lesson.status === 'finished' && me && !me.absent && !me.studentRated
    this.setData({
      lesson,
      me,
      statusText: STATUS_TEXT[lesson.status] || lesson.status,
      canRate
    })
  },

  onStar(e) {
    this.setData({ score: e.currentTarget.dataset.score })
  },

  onComment(e) {
    this.setData({ comment: e.detail.value })
  },

  onSubmit() {
    const studentId = auth.getCurrentStudentId()
    const result = lessonsService.rateByStudent(
      this.data.id,
      studentId,
      this.data.score,
      this.data.comment
    )
    wx.showToast({ title: result.message || (result.ok ? '已评价' : '失败'), icon: 'none' })
    this.refresh()
  },

  onSkip() {
    wx.navigateBack({ fail: () => wx.reLaunch({ url: '/pages/student/home/home' }) })
  }
})

