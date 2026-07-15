const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
const studentsService = require('../../../services/students')

Page({
  data: {
    courses: [],
    studentName: ''
  },
  onShow() {
    if (!auth.requireAuth()) return
    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)
    this.setData({
      courses: mock.getCoursesForStudent(studentId),
      studentName: student ? student.studentName : '学员'
    })
  }
})
