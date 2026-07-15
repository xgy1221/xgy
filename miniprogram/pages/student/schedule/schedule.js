const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
const studentsService = require('../../../services/students')

Page({
  data: {
    list: [],
    studentName: ''
  },
  onShow() {
    if (!auth.requireAuth()) return
    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)
    this.setData({
      list: mock.getScheduleForStudent(studentId),
      studentName: student ? student.studentName : '学员'
    })
  }
})
