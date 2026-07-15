const auth = require('./utils/auth')
const studentsService = require('./services/students')

App({
  globalData: {
    session: null,
    brandName: '学管云'
  },

  onLaunch() {
    studentsService.getAllStudents()
    this.globalData.session = auth.getSession()
  }
})
