const auth = require('../../../utils/auth')
const lessonsService = require('../../../services/lessons')

Page({
  data: {
    lessonId: '',
    lesson: {},
    keyword: '',
    mode: 'suggest',
    list: [],
    total: 0,
    autoFocus: true
  },

  onLoad(query) {
    if (!auth.requireAuth()) return
    const lessonId = query.lessonId || ''
    const lesson = lessonsService.getLessonById(lessonId) || {}
    this.setData({ lessonId, lesson })
    this.runSearch('')
  },

  runSearch(keyword) {
    const result = lessonsService.searchMakeupCandidates(this.data.lessonId, keyword)
    this.setData({
      keyword,
      mode: result.mode,
      list: result.list,
      total: result.total
    })
  },

  onKeyword(e) {
    const keyword = e.detail.value || ''
    this.runSearch(keyword)
  },

  onSearch() {
    this.runSearch(this.data.keyword)
  },

  onClear() {
    this.runSearch('')
  },

  onAdd(e) {
    const { id, name } = e.currentTarget.dataset
    wx.showModal({
      title: `确认临时加入？`,
      content: `将「${name}」加入本节课。只影响本课次，原班不变。`,
      confirmText: '加入本课',
      success: async (res) => {
        if (!res.confirm) return
        const api = require('../../../services/api')
        wx.showLoading({ title: '加入中', mask: true })
        try {
          if (api.isRemoteSession()) {
            await api.makeupStudent(this.data.lessonId, {
              studentId: Number(id)
            })
          }
          const result = lessonsService.addTempMakeupStudent(this.data.lessonId, id)
          if (!result.ok) {
            wx.showToast({ title: result.message || '加入失败', icon: 'none' })
            return
          }
          wx.showToast({ title: '已加入本课', icon: 'success' })
          setTimeout(() => {
            wx.navigateBack({
              fail: () =>
                wx.redirectTo({
                  url: `/pages/teacher/lesson-detail/lesson-detail?id=${this.data.lessonId}`
                })
            })
          }, 400)
        } catch (err) {
          wx.showToast({ title: (err && err.message) || '加入失败', icon: 'none' })
        } finally {
          wx.hideLoading()
        }
      }
    })
  }
})

