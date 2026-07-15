const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
const studentsService = require('../../../services/students')
const enrollmentsService = require('../../../services/enrollments')

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
    const session = auth.getSession()
    if (session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
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
    } else {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }

    const current = children.find((c) => c.id === currentStudentId) || children[0]
    const enrolls = enrollmentsService.getEnrollmentsByStudent(currentStudentId)
    const schedule = mock.getScheduleForStudent(currentStudentId)

    this.setData({
      parentName: u.name,
      phone: u.phone,
      children,
      currentStudentId,
      studentName: current.studentName,
      grade: current.grade || '',
      campus: current.campus || '',
      courseCount: enrolls.length,
      weekLessons: schedule.filter((s) => s.date !== '待排课').length,
      today: schedule.filter((s) => s.date === '今天'),
      todos: [
        `${current.studentName}：可在「课程」查看教案剩余课次`,
        children.length > 1 ? `本账号共 ${children.length} 名孩子，可上方切换` : '可在「我的」继续添加孩子',
        enrolls.length ? '课时不足时将提醒家长' : '尚未选择教案，可去课程页补选'
      ]
    })
  },

  onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id || id === this.data.currentStudentId) return
    auth.setCurrentStudentId(id)
    this.refresh()
  }
})
