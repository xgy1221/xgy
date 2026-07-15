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
    source: '机构导入'
  },
  {
    id: 'en_yn_en',
    studentId: 'stu_yn',
    packageId: 'pkg_en_24',
    totalLessons: 24,
    remainLessons: 24,
    status: '学习中',
    source: '机构导入'
  },
  {
    id: 'en_yr_write',
    studentId: 'stu_yr',
    packageId: 'pkg_write_16',
    totalLessons: 16,
    remainLessons: 10,
    status: '学习中',
    source: '机构导入'
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
  return getEnrollmentsByStudent(studentId).map((e) => {
    const pkg = packagesService.getPackageById(e.packageId) || {}
    return {
      ...e,
      packageName: pkg.name || '未知教案',
      subject: pkg.subject || '',
      grade: pkg.grade || '',
      price: pkg.price || 0,
      outline: pkg.outline || []
    }
  })
}

function enrollPackagesForStudent(studentId, packageIds, source) {
  const list = getAllEnrollments()
  const created = []
  ;(packageIds || []).forEach((packageId) => {
    const pkg = packagesService.getPackageById(packageId)
    if (!pkg) return
    const existed = list.find((e) => e.studentId === studentId && e.packageId === packageId)
    if (existed) return
    const item = {
      id: `en_${studentId}_${packageId}_${Date.now()}`,
      studentId,
      packageId,
      totalLessons: pkg.lessonCount,
      remainLessons: pkg.lessonCount,
      status: '待排课',
      source: source || '家长自选'
    }
    list.push(item)
    created.push(item)
  })
  saveAll(list)
  return created
}

module.exports = {
  getAllEnrollments,
  getEnrollmentsByStudent,
  getEnrollmentsDetailedByStudent,
  enrollPackagesForStudent
}
