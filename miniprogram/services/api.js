/**
 * 后端 API 适配层。
 * useRemote:
 *   - false  始终本地
 *   - true   始终远程（失败则报错）
 *   - 'auto' 探测后端，可用则远程，否则本地回退
 *
 * 真机调试请把 baseUrl 改成电脑局域网 IP，如 http://192.168.1.8:8080
 * 并在微信开发者工具勾选「不校验合法域名」。
 */
const auth = require('../utils/auth')

const config = {
  useRemote: 'auto',
  baseUrl: 'http://localhost:8080',
  roleToBackend: {
    student: 'PARENT',
    teacher: 'TEACHER',
    academic: 'ACADEMIC',
    partner: 'PARTNER',
    admin: 'ADMIN'
  },
  roleFromBackend: {
    PARENT: 'student',
    TEACHER: 'teacher',
    ACADEMIC: 'academic',
    PARTNER: 'partner',
    ADMIN: 'admin'
  }
}

let remoteReady = null
let probeAt = 0
const PROBE_TTL_MS = 30000

function roleFromBackend(role) {
  return config.roleFromBackend[role] || String(role || '').toLowerCase()
}

function roleToBackend(role) {
  return config.roleToBackend[role] || String(role || '').toUpperCase()
}

function mapLessonStatus(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'SCHEDULED') return 'upcoming'
  if (s === 'ONGOING') return 'ongoing'
  if (s === 'FINISHED') return 'finished'
  const lower = String(status || '').toLowerCase()
  if (lower === 'upcoming' || lower === 'ongoing' || lower === 'finished') return lower
  return 'upcoming'
}

function mapEnrollmentStatus(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'ACTIVE') return '学习中'
  if (s === 'FINISHED' || s === 'COMPLETED') return '已结业'
  if (s === 'SUSPENDED') return '暂停'
  return status || '学习中'
}

function asStr(v) {
  if (v == null || v === '') return ''
  return String(v)
}

function asTime(v) {
  if (v == null) return ''
  const s = String(v)
  return s.length >= 5 ? s.slice(0, 5) : s
}

function asDate(v) {
  if (v == null) return ''
  return String(v).slice(0, 10)
}

function getToken() {
  const session = auth.getSession()
  return (session && session.token) || ''
}

function isRemoteSession() {
  const session = auth.getSession()
  return !!(session && session.remote)
}

function probe() {
  const now = Date.now()
  if (remoteReady !== null && now - probeAt < PROBE_TTL_MS) {
    return Promise.resolve(remoteReady)
  }
  if (config.useRemote === false) {
    remoteReady = false
    probeAt = now
    return Promise.resolve(false)
  }
  if (config.useRemote === true) {
    remoteReady = true
    probeAt = now
    return Promise.resolve(true)
  }
  return new Promise((resolve) => {
    wx.request({
      url: `${config.baseUrl}/api/ping`,
      method: 'GET',
      timeout: 2500,
      success(res) {
        const body = res.data || {}
        remoteReady = res.statusCode === 200 && (body.code === 0 || body.data)
        probeAt = Date.now()
        resolve(remoteReady)
      },
      fail() {
        remoteReady = false
        probeAt = Date.now()
        resolve(false)
      }
    })
  })
}

function shouldUseRemote() {
  if (config.useRemote === false) return Promise.resolve(false)
  if (config.useRemote === true) return Promise.resolve(true)
  return probe()
}

function request({ path, method = 'GET', data, token }) {
  return new Promise((resolve, reject) => {
    const authToken = token != null ? token : getToken()
    wx.request({
      url: `${config.baseUrl}${path}`,
      method,
      data,
      timeout: 12000,
      header: {
        'Content-Type': 'application/json',
        Authorization: authToken ? `Bearer ${authToken}` : ''
      },
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 401) {
          auth.clearSession()
          wx.reLaunch({ url: '/pages/login/login' })
          reject(new Error('未登录'))
          return
        }
        if (res.statusCode >= 400) {
          reject(new Error((body && body.message) || `HTTP ${res.statusCode}`))
          return
        }
        if (body.code !== 0 && body.code !== undefined) {
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve(body.data !== undefined ? body.data : body)
      },
      fail(err) {
        reject(err && err.errMsg ? new Error(err.errMsg) : err || new Error('网络错误'))
      }
    })
  })
}

function login(phone, smsCode = '123456') {
  return request({ path: '/api/auth/login', method: 'POST', data: { phone, smsCode }, token: '' })
}

function fetchMe() {
  return request({ path: '/api/auth/me' })
}

function switchRole(role, orgId) {
  const payload = { role: roleToBackend(role) }
  if (orgId != null && orgId !== '') {
    const n = Number(orgId)
    if (!Number.isNaN(n)) payload.orgId = n
  }
  return request({ path: '/api/auth/switch-role', method: 'POST', data: payload })
}

function switchStudent(studentId) {
  return request({
    path: '/api/auth/switch-student',
    method: 'POST',
    data: { studentId: Number(studentId) }
  })
}

function logout() {
  return request({ path: '/api/auth/logout', method: 'POST', data: {} })
}

function fetchOrgs() {
  return request({ path: '/api/orgs' })
}

function fetchStudents(phone) {
  const q = phone ? `?phone=${encodeURIComponent(phone)}` : ''
  return request({ path: `/api/students${q}` })
}

function fetchPackages() {
  return request({ path: '/api/packages' })
}

function fetchEnrollments(studentId) {
  return request({ path: `/api/enrollments?studentId=${studentId}` })
}

function fetchLessonsByRange(from, to) {
  return request({ path: `/api/lessons?from=${from}&to=${to}` })
}

function fetchLessonsByDate(date) {
  const q = date ? `?date=${date}` : ''
  return request({ path: `/api/lessons${q}` })
}

function fetchLessonDetail(id) {
  return request({ path: `/api/lessons/${id}` })
}

function fetchStudentPackageLessons(studentId, packageId) {
  return request({
    path: `/api/lessons/student-package?studentId=${studentId}&packageId=${packageId}`
  })
}

function fetchActivities(tab, studentId) {
  let path = `/api/activities?tab=${tab || 'open'}`
  if (studentId) path += `&studentId=${studentId}`
  return request({ path })
}

function fetchActivityDetail(id) {
  return request({ path: `/api/activities/${id}` })
}

function signupActivity(activityId, studentId) {
  return request({
    path: `/api/activities/${activityId}/signup`,
    method: 'POST',
    data: { studentId: Number(studentId) }
  })
}

function cancelActivitySignup(signupId) {
  return request({
    path: `/api/activities/signups/${signupId}/cancel`,
    method: 'POST',
    data: {}
  })
}

function rateByTeacher(lessonId, payload) {
  return request({
    path: `/api/lessons/${lessonId}/rate-by-teacher`,
    method: 'POST',
    data: payload
  })
}

function rateByStudent(lessonId, payload) {
  return request({
    path: `/api/lessons/${lessonId}/rate-by-student`,
    method: 'POST',
    data: payload
  })
}

function makeupStudent(lessonId, payload) {
  return request({
    path: `/api/lessons/${lessonId}/makeup`,
    method: 'POST',
    data: payload
  })
}

function finishLesson(lessonId) {
  return request({
    path: `/api/lessons/${lessonId}/finish`,
    method: 'POST',
    data: {}
  })
}

function markAbsent(lessonId, payload) {
  return request({
    path: `/api/lessons/${lessonId}/absent`,
    method: 'POST',
    data: payload
  })
}

function fetchWhitelist() {
  return request({ path: '/api/whitelist', method: 'GET' })
}

function addWhitelist(payload) {
  return request({ path: '/api/whitelist', method: 'POST', data: payload })
}

function removeWhitelist(id) {
  return request({ path: `/api/whitelist/${id}`, method: 'DELETE' })
}

function fetchEnrollments(studentId) {
  const q = studentId != null ? `?studentId=${studentId}` : ''
  return request({ path: `/api/enrollments${q}`, method: 'GET' })
}

function upsertEnrollment(payload) {
  return request({ path: '/api/enrollments', method: 'POST', data: payload })
}

function fetchFinanceSummary() {
  return request({ path: '/api/finance/summary', method: 'GET' })
}

/**
 * 后端登录载荷 → 小程序 session
 * orgId 在 session / user 上使用机构 code，便于本地服务过滤；numericOrgId 留给写接口。
 */
function mapAuthToSession(data) {
  const rolesRaw = data.roles || []
  const roleKeys = []
  rolesRaw.forEach((r) => {
    const key = roleFromBackend(typeof r === 'string' ? r : r.role)
    if (key && roleKeys.indexOf(key) < 0) roleKeys.push(key)
  })

  const staffRole = rolesRaw.find((r) => {
    const role = typeof r === 'string' ? r : r.role
    return role && role !== 'PARENT' && r.orgCode
  })

  const orgCode = data.orgCode || (staffRole && staffRole.orgCode) || null
  const numericOrgId = data.orgId != null ? data.orgId : null

  const user = {
    id: asStr(data.userId),
    name: data.name || '用户',
    phone: data.phone,
    avatarText: data.avatarText || (data.name || '用').slice(0, 1),
    roles: roleKeys,
    orgId: staffRole ? staffRole.orgCode : null,
    numericOrgId: staffRole && staffRole.orgId != null ? staffRole.orgId : numericOrgId,
    title: '',
    campus: ''
  }

  const currentRole = roleFromBackend(data.currentRole)
  const currentStudentId = data.currentStudentId != null ? asStr(data.currentStudentId) : null
  const needsOnboarding = currentRole === 'student' && !(data.students && data.students.length)

  return {
    token: data.token,
    user,
    currentRole: currentRole || null,
    currentStudentId,
    currentOrgId: orgCode,
    numericOrgId,
    needsOnboarding,
    remote: true,
    remoteStudents: (data.students || []).map(normalizeStudent),
    loggedAt: Date.now()
  }
}

function normalizeStudent(s) {
  return {
    id: asStr(s.id),
    orgId: s.orgCode || asStr(s.orgId),
    orgNumericId: s.orgId,
    orgCode: s.orgCode || '',
    orgName: s.orgName || '',
    orgShortName: (s.orgName || '').replace(/思维|英语|教育|培训/g, '').slice(0, 2) || s.orgName || '',
    studentName: s.studentName,
    parentPhone: s.parentPhone || '',
    parentName: s.parentName || '',
    grade: s.grade || '',
    campus: s.campus || '',
    remark: s.remark || '',
    gender: s.gender || '',
    status: s.status || 'ACTIVE'
  }
}

function normalizePackage(p, orgCode) {
  let outline = p.outline
  if (typeof outline === 'string' && outline) {
    outline = outline.split(/;|；|\n/).map((x) => x.trim()).filter(Boolean)
  }
  if (!Array.isArray(outline)) outline = []
  return {
    id: asStr(p.id),
    orgId: orgCode || asStr(p.orgId),
    orgNumericId: p.orgId,
    name: p.name,
    subject: p.subject || '',
    grade: p.grade || '',
    lessonCount: p.lessonCount,
    price: p.price,
    status: p.status,
    outline
  }
}

function normalizeEnrollment(e, orgCode) {
  const total = Number(e.totalLessons) || 0
  const remain = Number(e.remainLessons) || 0
  const used = e.usedLessons != null ? Number(e.usedLessons) : Math.max(0, total - remain)
  return {
    id: asStr(e.id),
    orgId: orgCode || asStr(e.orgId),
    orgNumericId: e.orgId,
    studentId: asStr(e.studentId),
    packageId: asStr(e.packageId),
    packageName: e.packageName || '',
    subject: e.subject || '',
    grade: e.grade || '',
    price: e.price,
    totalLessons: total,
    remainLessons: remain,
    usedLessons: used,
    progress: e.progress != null ? Math.round(Number(e.progress)) : total ? Math.round((used / total) * 100) : 0,
    status: mapEnrollmentStatus(e.status),
    source: e.source || ''
  }
}

function normalizeLesson(l, orgCode) {
  const attendees = (l.attendees || []).map((a) => ({
    id: asStr(a.id),
    studentId: asStr(a.studentId),
    studentName: a.studentName || '',
    type: String(a.type || 'REGULAR').toLowerCase() === 'makeup' ? 'makeup' : 'regular',
    homeClassId: asStr(a.homeClassId),
    enrollmentId: asStr(a.enrollmentId),
    teacherRated: a.teacherRating != null || !!a.teacherRated,
    studentRated: a.studentRating != null || !!a.studentRated,
    teacherRating:
      a.teacherRating != null
        ? typeof a.teacherRating === 'object'
          ? a.teacherRating
          : { score: a.teacherRating, comment: a.teacherComment || '' }
        : null,
    studentRating:
      a.studentRating != null
        ? typeof a.studentRating === 'object'
          ? a.studentRating
          : { score: a.studentRating, comment: a.studentComment || '' }
        : null,
    consumed: !!a.consumed,
    absent: !!a.absent
  }))

  return {
    id: asStr(l.id),
    orgId: orgCode || asStr(l.orgId),
    orgNumericId: l.orgId,
    date: asDate(l.lessonDate || l.date),
    startTime: asTime(l.startTime),
    endTime: asTime(l.endTime),
    classId: asStr(l.classId),
    className: l.className || '',
    teacherId: asStr(l.teacherId),
    teacherName: l.teacherName || '',
    packageId: asStr(l.packageId),
    packageName: l.packageName || '',
    room: l.room || '',
    status: mapLessonStatus(l.status),
    attendees
  }
}

function normalizePackageLessonRow(l) {
  const score = l.teacherScore != null ? Number(l.teacherScore) : 0
  const hasEval = !!l.hasTeacherEval
  return {
    id: asStr(l.id),
    date: asDate(l.date),
    startTime: asTime(l.startTime),
    endTime: asTime(l.endTime),
    status: mapLessonStatus(l.status),
    statusText:
      mapLessonStatus(l.status) === 'finished'
        ? '已结束'
        : mapLessonStatus(l.status) === 'ongoing'
          ? '进行中'
          : '未开始',
    teacherName: l.teacherName || '',
    className: l.className || '',
    room: l.room || '',
    packageId: asStr(l.packageId),
    packageName: l.packageName || '',
    type: 'regular',
    consumed: !!l.consumed,
    teacherRated: hasEval,
    teacherScore: score,
    teacherComment: l.teacherComment || '',
    teacherStars: hasEval
      ? `${'★'.repeat(Math.max(0, Math.min(5, score)))}${'☆'.repeat(5 - Math.max(0, Math.min(5, score)))}`
      : '',
    hasTeacherEval: hasEval
  }
}

function normalizeActivity(a, orgCode, studentId) {
  const today = new Date()
  const y = today.getFullYear()
  const m = `${today.getMonth() + 1}`.padStart(2, '0')
  const d = `${today.getDate()}`.padStart(2, '0')
  const todayKey = `${y}-${m}-${d}`
  const startDate = asDate(a.startDate)
  const enrollDeadline = asDate(a.enrollDeadline)
  const isPast = startDate < todayKey
  const deadlinePassed = enrollDeadline < todayKey
  const enrolled = Number(a.signupCount != null ? a.signupCount : a.enrolledCount) || 0
  const capacity = Number(a.capacity) || 0
  const remain = a.remainSlots != null ? Number(a.remainSlots) : Math.max(0, capacity - enrolled)
  let enrollStatus = 'open'
  if (isPast) enrollStatus = 'past'
  else if (deadlinePassed) enrollStatus = 'closed'
  else if (capacity > 0 && enrolled >= capacity) enrollStatus = 'full'

  const statusTextMap = {
    open: '报名中',
    full: '已满员',
    closed: '已截止',
    past: '已完赛'
  }
  const fee = Number(a.fee) || 0
  const signed = !!(a.enrolled || a.signed || a.signupId)

  return {
    id: asStr(a.id),
    orgId: orgCode || asStr(a.orgId),
    orgNumericId: a.orgId,
    title: a.title,
    category: a.category || '',
    coverTone: a.coverTone || 'teal',
    campus: a.campus || '',
    address: a.address || '',
    startDate,
    startTime: asTime(a.startTime),
    endTime: asTime(a.endTime),
    enrollDeadline,
    capacity,
    fee,
    targetGrade: a.targetGrade || '',
    summary: a.summary || '',
    highlights: Array.isArray(a.highlights)
      ? a.highlights
      : String(a.highlights || '')
          .split(/[,，;；]/
          .map((x) => x.trim())
          .filter(Boolean),
    gallery: Array.isArray(a.gallery)
      ? a.gallery
      : String(a.gallery || '')
          .split(/[,，;；]/
          .map((x) => x.trim())
          .filter(Boolean),
    recap: a.recap || '',
    published: a.published !== false,
    enrolled,
    remain,
    enrollStatus,
    statusText: statusTextMap[enrollStatus],
    feeText: fee > 0 ? `¥${fee}` : '免费',
    dateText: `${startDate} ${asTime(a.startTime)}-${asTime(a.endTime)}`,
    deadlineText: enrollDeadline,
    signed,
    signupId: a.signupId != null ? asStr(a.signupId) : '',
    signupStatus: a.signupStatus || '',
    studentId: a.studentId != null ? asStr(a.studentId) : studentId || '',
    studentName: a.studentName || '',
    isPast,
    coverLabel: a.category || '比赛'
  }
}

module.exports = {
  config,
  probe,
  shouldUseRemote,
  isRemoteSession,
  request,
  login,
  fetchMe,
  switchRole,
  switchStudent,
  logout,
  fetchOrgs,
  fetchStudents,
  fetchPackages,
  fetchEnrollments,
  fetchLessonsByRange,
  fetchLessonsByDate,
  fetchLessonDetail,
  fetchStudentPackageLessons,
  fetchActivities,
  fetchActivityDetail,
  signupActivity,
  cancelActivitySignup,
  rateByTeacher,
  rateByStudent,
  makeupStudent,
  finishLesson,
  markAbsent,
  fetchWhitelist,
  addWhitelist,
  removeWhitelist,
  fetchEnrollments,
  upsertEnrollment,
  fetchFinanceSummary,
  mapEnrollmentStatus,
  mapAuthToSession,
  normalizeStudent,
  normalizePackage,
  normalizeEnrollment,
  normalizeLesson,
  normalizePackageLessonRow,
  normalizeActivity,
  roleFromBackend,
  roleToBackend,
  mapLessonStatus,
  asStr,
  asDate,
  asTime
}
