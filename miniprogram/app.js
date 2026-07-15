const auth = require('./utils/auth')
const studentsService = require('./services/students')
const packagesService = require('./services/packages')
const enrollmentsService = require('./services/enrollments')
const parentsService = require('./services/parents')
const lessonsService = require('./services/lessons')

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
    lessonsService.ensureClasses()
    lessonsService.ensureLessons()
    this.globalData.session = auth.getSession()
  }
})
