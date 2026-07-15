const auth = require('../../../utils/auth')
const parentsService = require('../../../services/parents')
const studentsService = require('../../../services/students')

Page({
  data: {
    phone: '',
    parentName: '',
    note: '',
    list: [],
    total: 0,
    pending: 0
  },

  onShow() {
    if (!auth.requireAuth()) return
    this.refresh()
  },

  refresh() {
    const list = parentsService.getWhitelist().map((item) => {
      const students = studentsService.getStudentsByPhone(item.phone)
      return {
        ...item,
        hasStudent: students.length > 0,
        studentText: students.length
          ? students.map((s) => s.studentName).join('、')
          : '暂无'
      }
    })
    this.setData({
      list,
      total: list.length,
      pending: list.filter((i) => !i.hasStudent).length
    })
  },

  onPhone(e) {
    this.setData({ phone: e.detail.value })
  },
  onName(e) {
    this.setData({ parentName: e.detail.value })
  },
  onNote(e) {
    this.setData({ note: e.detail.value })
  },

  onAdd() {
    const phone = (this.data.phone || '').trim()
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    const result = parentsService.addPhoneToWhitelist(
      phone,
      this.data.parentName.trim(),
      this.data.note.trim()
    )
    if (!result.ok) {
      wx.showToast({ title: result.message, icon: 'none' })
      return
    }
    this.setData({ phone: '', parentName: '', note: '' })
    this.refresh()
    wx.showToast({ title: '已加入', icon: 'success' })
  }
})
