const auth = require('../../../utils/auth')
const lessonsService = require('../../../services/lessons')

const STATUS_TEXT = {
  upcoming: '未开始',
  ongoing: '进行中',
  finished: '已结束'
}

function starsText(score) {
  const n = Number(score) || 0
  return `${'★'.repeat(n)}${'☆'.repeat(Math.max(0, 5 - n))} ${n}分`
}

Page({
  data: {
    id: '',
    lesson: {},
    teacherAvatar: '',
    statusText: '',
    summary: { present: 0, absent: 0, makeup: 0, consumed: 0 },
    rows: []
  },

  onLoad(query) {
    if (!auth.requireAuth()) return
    this.setData({ id: query.id || '' })
    this.refresh()
  },

  refresh() {
    const lesson = lessonsService.getLessonById(this.data.id)
    if (!lesson) {
      wx.showToast({ title: '课次不存在', icon: 'none' })
      return
    }

    const rows = (lesson.attendees || []).map((a) => ({
      ...a,
      studentStars: a.studentRating ? starsText(a.studentRating.score) : '',
      studentComment: a.studentRating ? a.studentRating.comment : '',
      teacherStars: a.teacherRating ? starsText(a.teacherRating.score) : '',
      teacherComment: a.teacherRating ? a.teacherRating.comment : ''
    }))

    const summary = {
      present: rows.filter((a) => !a.absent).length,
      absent: rows.filter((a) => a.absent).length,
      makeup: rows.filter((a) => a.type === 'makeup').length,
      consumed: rows.filter((a) => a.consumed).length
    }

    this.setData({
      lesson,
      teacherAvatar: (lesson.teacherName || '师').slice(0, 1),
      statusText: STATUS_TEXT[lesson.status] || lesson.status,
      summary,
      rows
    })
  }
})
