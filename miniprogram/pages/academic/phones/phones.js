const auth = require('../../../utils/auth')
const parentsService = require('../../../services/parents')
const studentsService = require('../../../services/students')
const api = require('../../../services/api')

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

  async refresh() {
    const orgId = auth.getCurrentOrgId()
    try {
      if (api.isRemoteSession()) {
        const rows = await api.fetchWhitelist()
        const list = (rows || []).map((item) => {
          const students = studentsService
            .getStudentsByPhone(item.phone)
            .filter((s) => s.orgId === orgId || s.orgNumericId != null)
          return {
            id: item.id,
            phone: item.phone,
            parentName: item.parentName || '',
            note: item.note || '',
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
        return
      }
    } catch (e) {
      wx.showToast({ title: (e && e.message) || '白名单加载失败', icon: 'none' })
    }

    const list = parentsService.getWhitelist(orgId).map((item) => {
      const students = studentsService
        .getStudentsByPhone(item.phone)
        .filter((s) => s.orgId === orgId)
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

  async onAdd() {
    const phone = (this.data.phone || '').trim()
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    try {
      if (api.isRemoteSession()) {
        await api.addWhitelist({
          phone,
          parentName: this.data.parentName.trim(),
          note: this.data.note.trim()
        })
      } else {
        const result = parentsService.addPhoneToWhitelist(
          phone,
          this.data.parentName.trim(),
          this.data.note.trim(),
          auth.getCurrentOrgId()
        )
        if (!result.ok) {
          wx.showToast({ title: result.message, icon: 'none' })
          return
        }
      }
      this.setData({ phone: '', parentName: '', note: '' })
      await this.refresh()
      wx.showToast({ title: '已加入', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: (e && e.message) || '加入失败', icon: 'none' })
    }
  }
})
