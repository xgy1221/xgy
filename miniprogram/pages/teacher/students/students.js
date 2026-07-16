const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')

Page({
  data: {
    keyword: '',
    list: [],
    filtered: [],
    total: 0
  },

  onShow() {
    if (!auth.requireAuth()) return
    this.reload()
  },

  reload() {
    const orgId = auth.getCurrentOrgId()
    const list = studentsService.listStudentsByOrg(orgId).map((s) => {
      const learning = enrollmentsService.summarizeStudentLearning(s.id)
      return {
        ...s,
        avatarText: (s.studentName || '学').slice(0, 1),
        phoneMask: maskPhone(s.parentPhone),
        packageCount: learning.packageCount,
        usedLessons: learning.usedLessons,
        totalLessons: learning.totalLessons,
        remainLessons: learning.remainLessons,
        summaryText: learning.summaryText,
        enrollments: learning.enrollments,
        expanded: false,
        lowRemain: learning.remainLessons > 0 && learning.remainLessons <= 4
      }
    })
    this.setData({ list, total: list.length })
    this.applyFilter(this.data.keyword, list)
  },

  onKeyword(e) {
    const keyword = e.detail.value || ''
    this.setData({ keyword })
    this.applyFilter(keyword, this.data.list)
  },

  onClear() {
    this.setData({ keyword: '' })
    this.applyFilter('', this.data.list)
  },

  applyFilter(keyword, source) {
    const kw = String(keyword || '')
      .trim()
      .toLowerCase()
    const list = source || this.data.list
    if (!kw) {
      this.setData({ filtered: list })
      return
    }
    const filtered = list.filter((s) => {
      const name = String(s.studentName || '').toLowerCase()
      const phone = String(s.parentPhone || '')
      const parent = String(s.parentName || '').toLowerCase()
      return name.indexOf(kw) >= 0 || phone.indexOf(kw) >= 0 || parent.indexOf(kw) >= 0
    })
    this.setData({ filtered })
  },

  onToggle(e) {
    const id = e.currentTarget.dataset.id
    const filtered = (this.data.filtered || []).map((s) =>
      s.id === id ? { ...s, expanded: !s.expanded } : s
    )
    const list = (this.data.list || []).map((s) =>
      s.id === id ? { ...s, expanded: !s.expanded } : s
    )
    this.setData({ filtered, list })
  },

  goSetup() {
    wx.navigateTo({ url: '/pages/academic/setup/setup' })
  },

  goEdit(e) {
    const { phone, id } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/academic/setup/setup?phone=${phone || ''}&studentId=${id || ''}`
    })
  }
})

function maskPhone(phone) {
  const p = String(phone || '')
  if (p.length < 7) return p || '-'
  return `${p.slice(0, 3)}****${p.slice(-4)}`
}
