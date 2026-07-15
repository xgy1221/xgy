const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const lessonsService = require('../../../services/lessons')

const STATUS_TEXT = {
  upcoming: '未开始',
  ongoing: '进行中',
  finished: '已结束'
}

Page({
  data: { list: [], studentName: '' },
  onShow() {
    if (!auth.requireAuth()) return
    const studentId = auth.getCurrentStudentId()
    const student = studentsService.getStudentById(studentId)
    const list = lessonsService
      .getAllLessons()
      .filter((l) => l.attendees.some((a) => a.studentId === studentId && !a.absent))
      .sort((a, b) => (a.date + a.startTime).localeCompare(b.date + b.startTime))
      .map((l) => ({ ...l, statusText: STATUS_TEXT[l.status] || l.status }))
    this.setData({
      list,
      studentName: student ? student.studentName : '学员'
    })
  },
  goDetail(e) {
    wx.navigateTo({
      url: `/pages/student/lesson-detail/lesson-detail?id=${e.currentTarget.dataset.id}`
    })
  }
})
