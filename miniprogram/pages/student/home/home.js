const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
const studentsService = require('../../../services/students')

Page({
  data: {
    parentName: '',
    phone: '',
    studentName: '',
    grade: '',
    campus: '',
    children: [],
    currentStudentId: '',
    courseCount: 0,
    weekLessons: 0,
    today: [],
    todos: []
  },

  onShow() {
    if (!auth.requireAuth()) return
    this.refresh()
  },

  refresh() {
    const session = auth.getSession()
    const u = session.user
    const children = studentsService.getStudentsByPhone(u.phone)
    let currentStudentId = session.currentStudentId

    if (children.length) {
      const exists = children.some((c) => c.id === currentStudentId)
      if (!exists) {
        currentStudentId = children[0].id
        auth.setCurrentStudentId(currentStudentId)
      }
    }

    const current = children.find((c) => c.id === currentStudentId) || children[0] || null
    const courses = mock.getCoursesForStudent(currentStudentId)
    const schedule = mock.getScheduleForStudent(currentStudentId)

    this.setData({
      parentName: u.name,
      phone: u.phone,
      children,
      currentStudentId,
      studentName: current ? current.studentName : '暂无绑定学员',
      grade: current ? current.grade : '',
      campus: current ? current.campus : '',
      courseCount: courses.filter((c) => c.status === '学习中').length,
      weekLessons: schedule.length,
      today: schedule.filter((s) => s.date === '今天'),
      todos: current
        ? [
            `${current.studentName}：有待完成练习`,
            children.length > 1 ? `本账号共 ${children.length} 名孩子，可上方切换` : '课时进度可在课程页查看',
            '课时不足时将提醒家长'
          ]
        : ['当前手机号尚未绑定学员，请联系教务导入']
    })
  },

  onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id || id === this.data.currentStudentId) return
    auth.setCurrentStudentId(id)
    this.refresh()
  }
})
