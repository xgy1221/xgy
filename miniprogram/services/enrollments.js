const packagesService = require('./packages')

const STORAGE_KEY = 'xgy_enrollments'

const SEED_ENROLLMENTS = [
  {
    id: 'en_yn_math',
    studentId: 'stu_yn',
    packageId: 'pkg_math_48',
    totalLessons: 48,
    remainLessons: 16,
    status: '学习中',
    source: '教务代录'
  },
  {
    id: 'en_yn_en',
    studentId: 'stu_yn',
    packageId: 'pkg_en_24',
    totalLessons: 24,
    remainLessons: 24,
    status: '学习中',
    source: '教务代录'
  },
  {
    id: 'en_yr_write',
    studentId: 'stu_yr',
    packageId: 'pkg_write_16',
    totalLessons: 16,
    remainLessons: 10,
    status: '学习中',
    source: '老师代录'
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

function getEnrollmentsByStudent(studentId) {
  return getAllEnrollments().filter((e) => e.studentId === studentId)
}

function getEnrollmentsDetailedByStudent(studentId) {
  return getEnrollmentsByStudent(studentId).map((e) => decorate(e))
}

function decorate(e) {
  const pkg = packagesService.getPackageById(e.packageId) || {}
  return {
    ...e,
    packageName: pkg.name || '未知教案',
    subject: pkg.subject || '',
    grade: pkg.grade || '',
    price: pkg.price || 0,
    outline: pkg.outline || []
  }
}

function getAllDetailedEnrollments() {
  return getAllEnrollments().map(decorate)
}

/**
 * 家长自选：批量新增，不覆盖已有
 */
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

/**
 * 教务/老师代录：可指定剩余课次、状态（适合在读学员）
 */
function upsertEnrollment(options) {
  const pkg = packagesService.getPackageById(options.packageId)
  if (!pkg) return { ok: false, message: '教案不存在' }
  if (!options.studentId) return { ok: false, message: '缺少学员' }

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

module.exports = {
  getAllEnrollments,
  getAllDetailedEnrollments,
  getEnrollmentsByStudent,
  getEnrollmentsDetailedByStudent,
  enrollPackagesForStudent,
  upsertEnrollment,
  updateRemainLessons
}
