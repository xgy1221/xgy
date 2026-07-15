const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
const studentsService = require('../../../services/students')

Page({
  data: { name: '', title: '', classCount: 0, studentCount: 0, today: [] },
  onShow() {
    if (!auth.requireAuth()) return
    const u = auth.getSession().user
    this.setData({
      name: u.name,
      title: u.title || '授课老师',
      classCount: mock.TEACHER_CLASSES.length,
      studentCount: studentsService.getAllStudents().length,
      today: mock.TEACHER_CLASSES.filter((c) => c.nextLesson.includes('今天'))
    })
  },
  goSetup() {
    wx.navigateTo({ url: '/pages/academic/setup/setup' })
  }
})
