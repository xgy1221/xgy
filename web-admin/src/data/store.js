import { request } from '../api/client'
import { getUser } from '../auth/roles'

export function todayKey() {
  const d = new Date()
  const p = (n) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function padTime(t) {
  if (!t) return ''
  const s = String(t)
  return s.length >= 5 ? s.slice(0, 5) : s
}

function mapStudentStatus(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'ACTIVE') return '在读'
  if (s === 'SUSPENDED') return '停课'
  if (s === 'FINISHED' || s === 'ARCHIVED') return '结业'
  if (status === '在读' || status === '停课' || status === '结业') return status
  return status || '在读'
}

function mapTeacherStatus(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'ACTIVE') return '在职'
  if (s === 'INACTIVE' || s === 'DISABLED') return '停用'
  if (status === '在职' || status === '停用') return status
  return status || '在职'
}

function mapPackageStatus(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'ON_SHELF') return '上架'
  if (s === 'OFF_SHELF') return '下架'
  if (status === '上架' || status === '下架') return status
  return status || '上架'
}

function mapOrderStatus(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'PAID') return '已缴费'
  if (s === 'PENDING') return '待缴费'
  if (s === 'REFUNDED') return '已退费'
  if (s === 'PARTIAL') return '部分缴费'
  return status || '-'
}

function mapLessonMark(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'FINISHED') return 'finished'
  return 'upcoming'
}

function scopeCampus(list, user) {
  if (!user || user.role !== 'partner' || !user.campus) return list
  return list.filter((row) => !row.campus || row.campus === user.campus)
}

function normalizeStudent(s) {
  return {
    ...s,
    id: s.id,
    status: mapStudentStatus(s.status),
    parentName: s.parentName || '家长',
    remark: s.remark || ''
  }
}

function normalizeTeacher(t) {
  return {
    ...t,
    id: t.id,
    status: mapTeacherStatus(t.status),
    subjects: t.subjects || '',
    title: t.title || ''
  }
}

function normalizePackage(p) {
  return {
    ...p,
    id: p.id,
    status: mapPackageStatus(p.status),
    price: Number(p.price) || 0,
    lessonCount: Number(p.lessonCount) || 0,
    outline: p.outline || ''
  }
}

function normalizeClass(c) {
  const studentIds = (c.studentIds || []).map(Number)
  return {
    ...c,
    id: c.id,
    packageId: c.packageId,
    teacherId: c.teacherId,
    packageName: c.packageName || '-',
    lessonCount: c.lessonCount || 0,
    packagePrice: Number(c.packagePrice) || 0,
    teacherName: c.teacherName || '-',
    studentIds,
    studentCount: c.studentCount != null ? c.studentCount : studentIds.length,
    students: (c.students || []).map((s) => ({
      id: s.id,
      studentName: s.studentName || '',
      parentPhone: s.parentPhone || '',
      campus: s.campus || ''
    }))
  }
}

function normalizeLesson(l) {
  const attendees = l.attendees || []
  return {
    ...l,
    id: l.id,
    date: l.lessonDate || l.date,
    startTime: padTime(l.startTime),
    endTime: padTime(l.endTime),
    className: l.className || '-',
    packageName: l.packageName || '-',
    teacherName: l.teacherName || '-',
    campus: l.campus || '',
    room: l.room || '',
    studentCount: attendees.length,
    status: mapLessonMark(l.status)
  }
}

export async function listCampuses(user = getUser()) {
  const rows = (await request('/api/campuses')) || []
  let list = rows.map((c) => ({ id: c.id, name: c.name, address: c.address || '' }))
  if (user?.role === 'partner' && user.campus) {
    list = list.filter((c) => c.name === user.campus)
    if (!list.length) list = [{ id: 'own', name: user.campus }]
  }
  return list
}

export async function listStudents(user = getUser()) {
  const rows = (await request('/api/students')) || []
  return scopeCampus(rows.map(normalizeStudent), user)
}

export async function upsertStudent(form) {
  const payload = {
    studentName: form.studentName,
    parentPhone: form.parentPhone,
    parentName: form.parentName || '',
    grade: form.grade || '',
    campus: form.campus || '',
    remark: form.remark || '',
    status: form.status || '在读'
  }
  if (form.id) {
    return normalizeStudent(await request(`/api/students/${form.id}`, { method: 'PUT', data: payload }))
  }
  return normalizeStudent(await request('/api/students', { method: 'POST', data: payload }))
}

export async function listTeachers(user = getUser()) {
  const rows = (await request('/api/teachers')) || []
  return scopeCampus(rows.map(normalizeTeacher), user)
}

export async function upsertTeacher(form) {
  const payload = {
    name: form.name,
    phone: form.phone || '',
    title: form.title || '',
    subjects: form.subjects || '',
    campus: form.campus || '',
    status: form.status === '停用' ? 'INACTIVE' : form.status === '在职' ? 'ACTIVE' : form.status || 'ACTIVE'
  }
  if (form.id) {
    return normalizeTeacher(await request(`/api/teachers/${form.id}`, { method: 'PUT', data: payload }))
  }
  return normalizeTeacher(await request('/api/teachers', { method: 'POST', data: payload }))
}

export async function listPackages() {
  const rows = (await request('/api/packages')) || []
  return rows.map(normalizePackage)
}

export async function upsertPackage(form) {
  const payload = {
    name: form.name,
    subject: form.subject || '',
    grade: form.grade || '',
    lessonCount: Number(form.lessonCount) || 0,
    price: Number(form.price) || 0,
    outline: form.outline || '',
    status: form.status || '上架'
  }
  if (form.id) {
    return normalizePackage(await request(`/api/packages/${form.id}`, { method: 'PUT', data: payload }))
  }
  return normalizePackage(await request('/api/packages', { method: 'POST', data: payload }))
}

export async function listClasses() {
  const rows = (await request('/api/classes')) || []
  return rows.map(normalizeClass)
}

export async function createClass(form) {
  try {
    const row = await request('/api/classes', {
      method: 'POST',
      data: {
        name: form.name,
        packageId: Number(form.packageId),
        teacherId: Number(form.teacherId),
        campus: form.campus || '',
        room: form.room || ''
      }
    })
    return { ok: true, data: normalizeClass(row) }
  } catch (e) {
    return { ok: false, message: e.message || '创建失败' }
  }
}

export async function addStudentToClass(classId, studentId) {
  try {
    await request(`/api/classes/${classId}/students`, {
      method: 'POST',
      data: { studentId: Number(studentId) }
    })
    return { ok: true }
  } catch (e) {
    return { ok: false, message: e.message || '加入失败' }
  }
}

export async function removeStudentFromClass(classId, studentId) {
  try {
    await request(`/api/classes/${classId}/students/${studentId}`, { method: 'DELETE' })
    return { ok: true }
  } catch (e) {
    return { ok: false, message: e.message || '移出失败' }
  }
}

function monthRangeAround(dateKey = todayKey()) {
  const d = new Date(`${dateKey}T00:00:00`)
  const from = new Date(d.getFullYear(), d.getMonth() - 2, 1)
  const to = new Date(d.getFullYear(), d.getMonth() + 3, 0)
  const p = (n) => (n < 10 ? `0${n}` : `${n}`)
  const fmt = (x) => `${x.getFullYear()}-${p(x.getMonth() + 1)}-${p(x.getDate())}`
  return { from: fmt(from), to: fmt(to) }
}

export async function getLessonDateMarks(anchorDate = todayKey()) {
  const { from, to } = monthRangeAround(anchorDate)
  const rows = (await request(`/api/lessons?from=${from}&to=${to}`)) || []
  const byDate = {}
  rows.forEach((l) => {
    const date = l.lessonDate
    if (!date) return
    const mark = mapLessonMark(l.status)
    if (!byDate[date] || mark === 'finished') byDate[date] = mark
  })
  return Object.keys(byDate).map((date) => ({ date, state: byDate[date] }))
}

export async function getLessonsByDate(date) {
  const rows = (await request(`/api/lessons?date=${date}`)) || []
  return rows.map(normalizeLesson)
}

export async function createLesson(form) {
  try {
    const row = await request('/api/lessons', {
      method: 'POST',
      data: {
        classId: Number(form.classId),
        teacherId: Number(form.teacherId),
        lessonDate: form.date || form.lessonDate,
        startTime: form.startTime.length === 5 ? `${form.startTime}:00` : form.startTime,
        endTime: form.endTime.length === 5 ? `${form.endTime}:00` : form.endTime,
        room: form.room || ''
      }
    })
    return { ok: true, data: normalizeLesson(row) }
  } catch (e) {
    return { ok: false, message: e.message || '订课失败' }
  }
}

export function formatMoney(n) {
  const v = Number(n) || 0
  return `¥${v.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

export async function getFinanceSummary(user = getUser()) {
  const [summary, orders] = await Promise.all([
    request('/api/finance/summary'),
    request('/api/finance/orders')
  ])
  const shareRatio =
    summary.shareRatio != null ? Number(summary.shareRatio) : user?.shareRatio || 0.15
  const received = Number(summary.paidAmount) || 0
  const refund = Number(summary.refundAmount) || 0
  const net = Number(summary.netAmount) || received - refund
  const share =
    summary.shareAmount != null ? Number(summary.shareAmount) : Math.round(net * shareRatio * 100) / 100

  return {
    sales: Number(summary.signedAmount) || 0,
    received,
    refund,
    receivable: Number(summary.pendingAmount) || 0,
    net,
    share,
    shareRatio,
    byCampus: (summary.byCampus || []).map((r) => ({
      campus: r.campus,
      received: Number(r.received) || 0,
      refund: Number(r.refund) || 0,
      net: Number(r.net) || 0
    })),
    byPackage: (summary.byPackage || []).map((r) => ({
      name: r.packageName || `教案#${r.packageId}`,
      count: r.count != null ? r.count : '-',
      received: Number(r.paidAmount) || 0
    })),
    orders: (orders || []).map((o) => ({
      id: o.id,
      createdAt: o.createdAt ? String(o.createdAt).slice(0, 10) : '',
      studentName: o.studentName || '-',
      parentPhone: o.parentPhone || '',
      packageName: o.packageName || '-',
      lessonCount: o.lessonCount || '',
      campus: o.campus || '',
      amount: Number(o.amountTotal) || 0,
      paidAmount: Number(o.amountPaid) || 0,
      refundAmount: Number(o.amountRefund) || 0,
      status: mapOrderStatus(o.status),
      channel: o.channel || '-'
    }))
  }
}

const ROLE_LABEL_CN = {
  PARENT: '家长',
  TEACHER: '老师',
  ACADEMIC: '教务',
  PARTNER: '合伙人',
  ADMIN: '管理员'
}

export async function listStaffAccounts() {
  const rows = (await request('/api/users')) || []
  return rows.map((u) => ({
    id: u.id,
    name: u.name,
    phone: u.phone,
    campus: u.campus || '-',
    status: u.status === 'ACTIVE' ? '正常' : u.status || '正常',
    roles: (u.roles || []).map((r) => ROLE_LABEL_CN[r.role] || r.role)
  }))
}

export async function getDashboardStats(user = getUser()) {
  const tasks = [
    listStudents(user),
    listTeachers(user),
    listClasses(),
    listPackages(),
    getLessonsByDate(todayKey())
  ]
  const canFinance = user?.role === 'partner' || user?.role === 'admin'
  if (canFinance) tasks.push(getFinanceSummary(user))

  const [students, teachers, classes, packages, todayLessons, finance] = await Promise.all(tasks)
  return {
    studentCount: students.length,
    teacherCount: teachers.length,
    classCount: classes.length,
    packageCount: packages.length,
    todayLessonCount: todayLessons.length,
    finance: finance || null
  }
}
