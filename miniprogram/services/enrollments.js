const packagesService = require('./packages')
const studentsService = require('./students')

const STORAGE_KEY = 'xgy_enrollments'

const SEED_ENROLLMENTS = [
  {
    id: 'en_xuequ_yn_math',
    orgId: 'org_xuequ',
    studentId: 'stu_xuequ_yn',
    packageId: 'pkg_xuequ_math_48',
    totalLessons: 48,
    remainLessons: 16,
    status: '学习中',
    source: '教务代录'
  },
  {
    id: 'en_xuequ_yn_en',
    orgId: 'org_xuequ',
    studentId: 'stu_xuequ_yn',
    packageId: 'pkg_xuequ_en_24',
    totalLessons: 24,
    remainLessons: 24,
    status: '学习中',
    source: '教务代录'
  },
  {
    id: 'en_xuequ_yr_write',
    orgId: 'org_xuequ',
    studentId: 'stu_xuequ_yr',
    packageId: 'pkg_xuequ_write_16',
    totalLessons: 16,
    remainLessons: 10,
    status: '学习中',
    source: '老师代录'
  },
  {
    id: 'en_qihang_yn_en',
    orgId: 'org_qihang',
    studentId: 'stu_qihang_yn',
    packageId: 'pkg_qihang_en_36',
    totalLessons: 36,
    remainLessons: 30,
    status: '学习中',
    source: '教务代录'
  }
]

function ensureSeed() {
  const existing = wx.getStorageSync(STORAGE_KEY)
  if (existing && Array.isArray(existing) && existing.length) return existing
  wx.setStorageSync(STORAGE_KEY, SEED_ENROLLMENTS)
  return SEED_ENROLLMENTS.slice()
}

function getAllEnrollments() {
  return ensureSeed().slice()
}

function saveAll(list) {
  wx.setStorageSync(STORAGE_KEY, list)
}

function listEnrollmentsByOrg(orgId) {
  if (!orgId) return []
  return getAllEnrollments().filter((e) => e.orgId === orgId)
}

function getEnrollmentsByStudent(studentId) {
  return getAllEnrollments().filter((e) => e.studentId === studentId)
}

function getEnrollmentsDetailedByStudent(studentId) {
  return getEnrollmentsByStudent(studentId).map((e) => decorate(e))
}

function decorate(e) {
  const pkg = packagesService.getPackageById(e.packageId) || {}
  const total = Number(e.totalLessons) || 0
  const remain = Number(e.remainLessons) || 0
  const used = Math.max(0, total - remain)
  const progress = total ? Math.round((used / total) * 100) : 0
  return {
    ...e,
    packageName: pkg.name || '未知教案',
    subject: pkg.subject || '',
    grade: pkg.grade || '',
    price: pkg.price || 0,
    outline: pkg.outline || [],
    usedLessons: used,
    progress,
    progressText: `${used}/${total}`,
    remainText: `剩余 ${remain}`
  }
}

/** 按学员汇总报读进度，供家长「我的」/ 老师查学员 */
function summarizeStudentLearning(studentId) {
  const list = getEnrollmentsDetailedByStudent(studentId)
  const totalLessons = list.reduce((s, e) => s + (Number(e.totalLessons) || 0), 0)
  const usedLessons = list.reduce((s, e) => s + (Number(e.usedLessons) || 0), 0)
  const remainLessons = list.reduce((s, e) => s + (Number(e.remainLessons) || 0), 0)
  return {
    enrollments: list,
    packageCount: list.length,
    totalLessons,
    usedLessons,
    remainLessons,
    summaryText: list.length
      ? `已上 ${usedLessons} / 共 ${totalLessons} 课 · 剩 ${remainLessons}`
      : '尚未报读教案'
  }
}

function getAllDetailedEnrollments(orgId) {
  const list = orgId ? listEnrollmentsByOrg(orgId) : getAllEnrollments()
  return list.map(decorate)
}

function enrollPackagesForStudent(studentId, packageIds, source) {
  const created = []
  ;(packageIds || []).forEach((packageId) => {
    const result = upsertEnrollment({
      studentId,
      packageId,
      source: source || '家长自选',
      status: '待排课',
      overwrite: false
    })
    if (result.ok && result.created) created.push(result.enrollment)
  })
  return created
}

function upsertEnrollment(options) {
  const pkg = packagesService.getPackageById(options.packageId)
  if (!pkg) return { ok: false, message: '教案不存在' }
  if (!options.studentId) return { ok: false, message: '缺少学员' }

  const student = studentsService.getStudentById(options.studentId)
  if (!student) return { ok: false, message: '学员不存在' }
  if (pkg.orgId && student.orgId && pkg.orgId !== student.orgId) {
    return { ok: false, message: '不能跨机构绑定教案' }
  }

  const orgId = student.orgId || pkg.orgId
  const list = getAllEnrollments()
  const existed = list.find(
    (e) => e.studentId === options.studentId && e.packageId === options.packageId
  )

  const totalLessons = Number(options.totalLessons != null ? options.totalLessons : pkg.lessonCount)
  let remainLessons =
    options.remainLessons != null ? Number(options.remainLessons) : totalLessons
  if (Number.isNaN(remainLessons) || remainLessons < 0) remainLessons = 0
  if (remainLessons > totalLessons) remainLessons = totalLessons

  if (existed) {
    if (options.overwrite === false) {
      return { ok: true, created: false, enrollment: existed }
    }
    Object.assign(existed, {
      orgId,
      totalLessons,
      remainLessons,
      status: options.status || existed.status || '学习中',
      source: options.source || existed.source || '教务代录',
      updatedAt: Date.now()
    })
    saveAll(list)
    return { ok: true, created: false, enrollment: existed }
  }

  const enrollment = {
    id: `en_${options.studentId}_${options.packageId}_${Date.now()}`,
    orgId,
    studentId: options.studentId,
    packageId: options.packageId,
    totalLessons,
    remainLessons,
    status: options.status || '学习中',
    source: options.source || '教务代录',
    createdAt: Date.now()
  }
  list.push(enrollment)
  saveAll(list)
  return { ok: true, created: true, enrollment }
}

function updateRemainLessons(enrollmentId, remainLessons) {
  const list = getAllEnrollments()
  const item = list.find((e) => e.id === enrollmentId)
  if (!item) return null
  let remain = Number(remainLessons)
  if (Number.isNaN(remain) || remain < 0) remain = 0
  if (remain > item.totalLessons) remain = item.totalLessons
  item.remainLessons = remain
  item.updatedAt = Date.now()
  saveAll(list)
  return item
}

function consumeOneLesson(enrollmentId) {
  const list = getAllEnrollments()
  const item = list.find((e) => e.id === enrollmentId)
  if (!item) return { ok: false, message: '报读不存在' }
  if (item.remainLessons <= 0) return { ok: false, message: '剩余课次不足' }
  item.remainLessons -= 1
  item.updatedAt = Date.now()
  saveAll(list)
  return { ok: true, enrollment: item }
}

function pickEnrollmentForPackage(studentId, packageId) {
  const list = getEnrollmentsByStudent(studentId)
  return (
    list.find((e) => e.packageId === packageId && e.remainLessons > 0) ||
    list.find((e) => e.remainLessons > 0) ||
    list[0] ||
    null
  )
}

module.exports = {
  getAllEnrollments,
  listEnrollmentsByOrg,
  getAllDetailedEnrollments,
  getEnrollmentsByStudent,
  getEnrollmentsDetailedByStudent,
  summarizeStudentLearning,
  enrollPackagesForStudent,
  upsertEnrollment,
  updateRemainLessons,
  consumeOneLesson,
  pickEnrollmentForPackage
}
