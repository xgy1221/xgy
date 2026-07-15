const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')

Page({
  data: {
    parentName: '',
    studentName: '',
    grade: '',
    courseCount: 0,
    weekLessons: 0,
    today: [],
    todos: []
  },
  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    const u = session.user
    this.setData({
      parentName: u.name,
      studentName: u.studentName || '学员',
      grade: u.grade || '',
      courseCount: mock.STUDENT_COURSES.filter(c => c.status === '学习中').length,
      weekLessons: mock.STUDENT_SCHEDULE.length,
      today: mock.STUDENT_SCHEDULE.filter(s => s.date === '今天'),
      todos: ['英语阅读作业待提交', '下周试听课确认通知', '课时剩余不足 20 小时']
    })
  }
})
