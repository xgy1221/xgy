const auth = require('../../../utils/auth')
const { parseCsv, rowsToObjects } = require('../../../utils/csv')
const studentsService = require('../../../services/students')

Page({
  data: {
    columns: [],
    note: '',
    families: [],
    totalStudents: 0,
    totalFamilies: 0,
    lastResult: null
  },

  onShow() {
    if (!auth.requireAuth()) return
    const tip = studentsService.getTemplateHint()
    this.setData({
      columns: tip.columns,
      note: tip.note
    })
    this.refreshFamilies()
  },

  refreshFamilies() {
    const orgId = auth.getCurrentOrgId()
    const families = studentsService.groupByPhone(studentsService.listStudentsByOrg(orgId))
    const totalStudents = families.reduce((sum, f) => sum + f.students.length, 0)
    this.setData({
      families,
      totalStudents,
      totalFamilies: families.length
    })
  },

  applyResult(result) {
    wx.showToast({
      title: `新增${result.created} 更新${result.updated}`,
      icon: 'none'
    })
    this.setData({ lastResult: result })
    this.refreshFamilies()
  },

  onDemoImport() {
    const result = studentsService.importDemoExcel(auth.getCurrentOrgId())
    this.applyResult(result)
  },

  onChooseCsv() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['csv', 'txt'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) return
        const fs = wx.getFileSystemManager()
        fs.readFile({
          filePath: file.path,
          encoding: 'utf-8',
          success: (readRes) => {
            try {
              const rows = rowsToObjects(parseCsv(readRes.data))
              if (!rows.length) {
                wx.showToast({ title: '文件无有效数据行', icon: 'none' })
                return
              }
              const result = studentsService.importStudentRows(rows, auth.getCurrentOrgId())
              this.applyResult(result)
            } catch (err) {
              wx.showToast({ title: '解析失败，请检查 CSV', icon: 'none' })
            }
          },
          fail: () => {
            wx.showToast({ title: '读取文件失败', icon: 'none' })
          }
        })
      }
    })
  },

  onReset() {
    wx.showModal({
      title: '恢复示例数据',
      content: '将清空已导入数据，恢复为初始学员列表。',
      success: (res) => {
        if (!res.confirm) return
        studentsService.resetToSeed()
        this.setData({ lastResult: null })
        this.refreshFamilies()
        wx.showToast({ title: '已恢复', icon: 'success' })
      }
    })
  }
})
