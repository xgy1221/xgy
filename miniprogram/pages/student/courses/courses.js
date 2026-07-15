const auth = require('../../../utils/auth')
const mock = require('../../../services/mock')
Page({
  data: { courses: [] },
  onShow() {
    if (!auth.requireAuth()) return
    this.setData({ courses: mock.STUDENT_COURSES })
  }
})
