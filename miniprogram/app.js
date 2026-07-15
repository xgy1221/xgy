const auth = require('./utils/auth')
const studentsService = require('./services/students')
const packagesService = require('./services/packages')
const enrollmentsService = require('./services/enrollments')
const parentsService = require('./services/parents')

App({
  globalData: {
    session: null,
    brandName: '学管云'
  },

  onLaunch() {
    studentsService.getAllStudents()
    packagesService.getAllPackages()
    enrollmentsService.getAllEnrollments()
    parentsService.getWhitelist()
    this.globalData.session = auth.getSession()
  }
})
