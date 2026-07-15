const auth = require('../../../utils/auth')
const lessonsService = require('../../../services/lessons')
const studentsService = require('../../../services/students')

Page({
  data: { list: [] },
  onShow() {
    if (!auth.requireAuth()) return
    lessonsService.ensureClasses()
    const list = lessonsService.getClasses().map((c) => ({
      ...c,
      students: (c.studentIds || []).length,
      studentNames: (c.studentIds || [])
        .map((id) => {
          const s = studentsService.getStudentById(id)
          return s ? s.studentName : id
        })
        .join('、'),
      nextLesson: '见课表日历'
    }))
    this.setData({ list })
  }
})
