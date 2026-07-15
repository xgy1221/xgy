const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
Page({
  data: { name:'', title:'', classCount:0, studentCount:0, today:[] },
  onShow() {
    if (!auth.requireAuth()) return
    const u = auth.getSession().user
    const students = mock.TEACHER_STUDENTS.length
    this.setData({
      name: u.name,
      title: u.title || '授课老师',
      classCount: mock.TEACHER_CLASSES.length,
      studentCount: students,
      today: mock.TEACHER_CLASSES.filter(c => c.nextLesson.includes('今天'))
    })
  }
})
