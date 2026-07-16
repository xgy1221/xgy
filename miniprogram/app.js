const auth = require('./utils/auth')
const schema = require('./services/schema')
const orgs = require('./services/orgs')
const studentsService = require('./services/students')
const packagesService = require('./services/packages')
const enrollmentsService = require('./services/enrollments')
const parentsService = require('./services/parents')
const lessonsService = require('./services/lessons')
const activitiesService = require('./services/activities')

App({
  globalData: {
    session: null,
    brandName: '学管云'
  },

  onLaunch() {
    schema.ensureSchema()
    orgs.ensureOrgs()
    studentsService.getAllStudents()
    packagesService.getAllPackages()
    enrollmentsService.getAllEnrollments()
    parentsService.getWhitelist()
    lessonsService.ensureClasses()
    lessonsService.ensureLessons()
    activitiesService.ensureActivities()
    this.globalData.session = auth.getSession()
  }
})
