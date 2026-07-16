const auth = require('../../../utils/auth')
const lessonsService = require('../../../services/lessons')
const motion = require('../../../utils/motion')

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
    motion.tap('medium')
    wx.navigateTo({
      url: `/pages/teacher/add-temp/add-temp?lessonId=${this.data.id}`
    })
  },

  async onToggleAbsent(e) {
    const { id, absent } = e.currentTarget.dataset
    const api = require('../../../services/api')
    motion.tap('light')
    wx.showLoading({ title: '更新中', mask: true })
    try {
      if (api.isRemoteSession()) {
        await api.markAbsent(this.data.id, {
          studentId: Number(id),
          absent: !!absent
        })
      }
      lessonsService.markAbsent(this.data.id, id, !!absent)
      this.refresh()
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '操作失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  onFinish() {
    motion.tap('light')
    wx.showModal({
      title: '确认下课？',
      content: '下课后请评价学生以消课。学生评价老师可选。临时插班不改变原班归属。',
      confirmText: '确认下课',
      success: async (res) => {
        if (!res.confirm) return
        const api = require('../../../services/api')
        motion.tap('medium')
        wx.showLoading({ title: '下课中', mask: true })
        try {
          if (api.isRemoteSession()) {
            await api.finishLesson(this.data.id)
          }
          const result = lessonsService.finishLesson(this.data.id)
          wx.showToast({ title: result.ok ? '已下课，请评价学生' : result.message, icon: 'none' })
          this.refresh()
        } catch (err) {
          wx.showToast({ title: (err && err.message) || '下课失败', icon: 'none' })
        } finally {
          wx.hideLoading()
        }
      }
    })
  },

  openRate(e) {
    motion.tap('light')
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
    motion.tap('light')
    this.setData({ rateScore: e.currentTarget.dataset.score })
  },

  onComment(e) {
    this.setData({ rateComment: e.detail.value })
  },

  async onSubmitRate() {
    const api = require('../../../services/api')
    motion.tap('medium')
    wx.showLoading({ title: '提交中', mask: true })
    try {
      if (api.isRemoteSession()) {
        await api.rateByTeacher(this.data.id, {
          studentId: Number(this.data.rateStudentId),
          rating: Number(this.data.rateScore) || 5,
          comment: this.data.rateComment || ''
        })
      }
      const result = lessonsService.rateByTeacher(
        this.data.id,
        this.data.rateStudentId,
        this.data.rateScore,
        this.data.rateComment
      )
      const msg = result.consumed
        ? '已评价并消 1 课'
        : result.message || (result.ok ? '已评价' : '失败')
      wx.showToast({ title: msg, icon: result.ok ? 'success' : 'none' })
      this.setData({ rateVisible: false })
      this.refresh()
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '评价失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  }
})

