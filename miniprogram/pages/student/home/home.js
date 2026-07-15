const auth = require('../../../utils/auth')
const studentsService = require('../../../services/students')
const lessonsService = require('../../../services/lessons')
const { todayKey, formatDisplay } = require('../../../utils/date')

const STATUS_TEXT = {
  upcoming: '未开始',
  ongoing: '进行中',
  finished: '已结束'
}

Page({
  data: {
    studentName: '',
    children: [],
    currentStudentId: '',
    selectedDate: '',
    displayDate: '',
    markedDates: [],
    lessons: []
  },

  onShow() {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    if (session.needsOnboarding) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    lessonsService.ensureLessons()
    const children = studentsService.getStudentsByPhone(session.user.phone)
    if (!children.length) {
      wx.reLaunch({ url: '/pages/onboarding/onboarding' })
      return
    }
    let currentStudentId = session.currentStudentId
    if (!children.some((c) => c.id === currentStudentId)) {
      currentStudentId = children[0].id
      auth.setCurrentStudentId(currentStudentId)
    }
    const selectedDate = this.data.selectedDate || todayKey()
    this.setData({
      children,
      currentStudentId,
      studentName: (children.find((c) => c.id === currentStudentId) || {}).studentName || '',
      selectedDate
    })
    this.refresh(currentStudentId, selectedDate)
  },

  refresh(studentId, date) {
    const markedDates = lessonsService.getLessonDatesForStudent(studentId)
    const lessons = lessonsService.getStudentLessonsByDate(studentId, date).map((l) => {
      const me = l.attendees.find((a) => a.studentId === studentId)
      return {
        ...l,
        statusText: STATUS_TEXT[l.status] || l.status,
        myType: me ? me.type : 'regular',
        homeClassName: me ? me.homeClassName : '',
        needRate: l.status === 'finished' && me && !me.studentRated && !me.absent
      }
    })
    this.setData({
      markedDates,
      lessons,
      displayDate: formatDisplay(date)
    })
  },

  onSelectDate(e) {
    const date = e.detail.date
    this.setData({ selectedDate: date })
    this.refresh(this.data.currentStudentId, date)
  },

  onSwitchChild(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    auth.setCurrentStudentId(id)
    const student = this.data.children.find((c) => c.id === id)
    this.setData({
      currentStudentId: id,
      studentName: student ? student.studentName : ''
    })
    this.refresh(id, this.data.selectedDate)
  },

  goDetail(e) {
    wx.navigateTo({
      url: `/pages/student/lesson-detail/lesson-detail?id=${e.currentTarget.dataset.id}`
    })
  }
})
