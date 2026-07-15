const { todayKey, addDays } = require('../utils/date')
const enrollmentsService = require('./enrollments')
const studentsService = require('./students')

const CLASS_KEY = 'xgy_classes'
const LESSON_KEY = 'xgy_lessons'

const SEED_CLASSES = [
  {
    id: 'class_math_a',
    name: '四年级数学 A 班',
    teacherId: 'u_teacher',
    teacherName: '李老师',
    packageId: 'pkg_math_48',
    packageName: '小学数学思维提升',
    room: 'A203',
    studentIds: ['stu_yn', 'stu_lz']
  },
  {
    id: 'class_write_b',
    name: '一年级书写 B 班',
    teacherId: 'u_teacher',
    teacherName: '李老师',
    packageId: 'pkg_write_16',
    packageName: '硬笔书写课',
    room: 'C102',
    studentIds: ['stu_yr', 'stu_cs']
  }
]

function buildAttendee(studentId, type, homeClass) {
  const stu = studentsService.getStudentById(studentId) || {}
  const enrollment = enrollmentsService.pickEnrollmentForPackage(
    studentId,
    homeClass.packageId
  )
  return {
    studentId,
    studentName: stu.studentName || '学员',
    type, // regular | makeup
    homeClassId: homeClass.id,
    homeClassName: homeClass.name,
    enrollmentId: enrollment ? enrollment.id : '',
    studentRated: false,
    teacherRated: false,
    studentRating: null,
    teacherRating: null,
    consumed: false,
    absent: false
  }
}

function buildSeedLessons() {
  studentsService.getAllStudents()
  enrollmentsService.getAllEnrollments()
  const today = todayKey()
  const classA = SEED_CLASSES[0]
  const classB = SEED_CLASSES[1]

  const lessonTodayA = {
    id: 'les_today_a',
    date: today,
    startTime: '16:00',
    endTime: '17:30',
    classId: classA.id,
    className: classA.name,
    teacherId: classA.teacherId,
    teacherName: classA.teacherName,
    packageId: classA.packageId,
    packageName: classA.packageName,
    room: classA.room,
    status: 'ongoing', // upcoming | ongoing | finished
    attendees: classA.studentIds.map((id) => buildAttendee(id, 'regular', classA))
  }

  const lessonTodayB = {
    id: 'les_today_b',
    date: today,
    startTime: '15:00',
    endTime: '16:00',
    classId: classB.id,
    className: classB.name,
    teacherId: classB.teacherId,
    teacherName: classB.teacherName,
    packageId: classB.packageId,
    packageName: classB.packageName,
    room: classB.room,
    status: 'upcoming',
    attendees: classB.studentIds.map((id) => {
      const row = buildAttendee(id, 'regular', classB)
      // 王一然上节旷课，本班正常学员仍在；可被临时插到数学班补课
      if (id === 'stu_yr') row.absent = false
      return row
    })
  }

  const lessonTomorrowA = {
    id: 'les_tmr_a',
    date: addDays(today, 1),
    startTime: '16:00',
    endTime: '17:30',
    classId: classA.id,
    className: classA.name,
    teacherId: classA.teacherId,
    teacherName: classA.teacherName,
    packageId: classA.packageId,
    packageName: classA.packageName,
    room: classA.room,
    status: 'upcoming',
    attendees: classA.studentIds.map((id) => buildAttendee(id, 'regular', classA))
  }

  const lessonYesterdayB = {
    id: 'les_yday_b',
    date: addDays(today, -2),
    startTime: '15:00',
    endTime: '16:00',
    classId: classB.id,
    className: classB.name,
    teacherId: classB.teacherId,
    teacherName: classB.teacherName,
    packageId: classB.packageId,
    packageName: classB.packageName,
    room: classB.room,
    status: 'finished',
    attendees: classB.studentIds.map((id) => {
      const row = buildAttendee(id, 'regular', classB)
      if (id === 'stu_yr') {
        row.absent = true
        row.consumed = false
      } else {
        row.studentRated = true
        row.teacherRated = true
        row.studentRating = { score: 5, comment: '认真' }
        row.teacherRating = { score: 5, comment: '专注' }
        row.consumed = true
      }
      return row
    })
  }

  const lessonWeekA = {
    id: 'les_week_a',
    date: addDays(today, 3),
    startTime: '10:00',
    endTime: '11:30',
    classId: classA.id,
    className: classA.name,
    teacherId: classA.teacherId,
    teacherName: classA.teacherName,
    packageId: classA.packageId,
    packageName: classA.packageName,
    room: classA.room,
    status: 'upcoming',
    attendees: classA.studentIds.map((id) => buildAttendee(id, 'regular', classA))
  }

  return [lessonYesterdayB, lessonTodayB, lessonTodayA, lessonTomorrowA, lessonWeekA]
}

function ensureClasses() {
  const existing = wx.getStorageSync(CLASS_KEY)
  if (existing && Array.isArray(existing) && existing.length) return existing
  wx.setStorageSync(CLASS_KEY, SEED_CLASSES)
  return SEED_CLASSES.slice()
}

function ensureLessons() {
  const existing = wx.getStorageSync(LESSON_KEY)
  if (existing && Array.isArray(existing) && existing.length) return existing
  const seeded = buildSeedLessons()
  wx.setStorageSync(LESSON_KEY, seeded)
  return seeded
}

function getClasses() {
  return ensureClasses().slice()
}

function getClassById(id) {
  return getClasses().find((c) => c.id === id) || null
}

function getAllLessons() {
  return ensureLessons().slice()
}

function saveLessons(list) {
  wx.setStorageSync(LESSON_KEY, list)
}

function getLessonById(id) {
  return getAllLessons().find((l) => l.id === id) || null
}

function getLessonsByDate(date) {
  return getAllLessons().filter((l) => l.date === date)
}

function getLessonDatesForStudent(studentId) {
  return getLessonDateMarksForStudent(studentId).map((m) => m.date)
}

function getLessonDatesForTeacher() {
  return getLessonDateMarksForTeacher().map((m) => m.date)
}

/**
 * 日历色标：finished 已上 / upcoming 未上 / mixed 当天既有已上又有未上
 */
function collectDateMarks(matchLesson) {
  const map = {}
  getAllLessons().forEach((l) => {
    if (!matchLesson(l)) return
    if (!map[l.date]) map[l.date] = { hasFinished: false, hasOpen: false }
    if (l.status === 'finished') map[l.date].hasFinished = true
    else map[l.date].hasOpen = true
  })
  return Object.keys(map)
    .sort()
    .map((date) => {
      const row = map[date]
      let state = 'upcoming'
      if (row.hasFinished && row.hasOpen) state = 'mixed'
      else if (row.hasFinished) state = 'finished'
      return { date, state }
    })
}

function getLessonDateMarksForStudent(studentId) {
  return collectDateMarks((l) =>
    l.attendees.some((a) => a.studentId === studentId && !a.absent)
  )
}

function getLessonDateMarksForTeacher() {
  return collectDateMarks(() => true)
}

function getStudentLessonsByDate(studentId, date) {
  return getAllLessons().filter(
    (l) =>
      l.date === date &&
      l.attendees.some((a) => a.studentId === studentId && !a.absent)
  )
}

function getTeacherLessonsByDate(date) {
  return getAllLessons().filter((l) => l.date === date)
}

/**
 * 临时插班补课：不改原班花名册，仅进入本课次 attendees
 * 下次教务排课仍按 homeClassId / 原班
 */
function addTempMakeupStudent(lessonId, studentId) {
  const list = getAllLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  if (lesson.status === 'finished') return { ok: false, message: '本课已结束' }
  if (lesson.attendees.some((a) => a.studentId === studentId)) {
    return { ok: false, message: '该学员已在本课名单中' }
  }

  const homeClass =
    getClasses().find((c) => (c.studentIds || []).indexOf(studentId) >= 0) ||
    getClassById(lesson.classId)
  if (!homeClass) return { ok: false, message: '找不到学员原班' }

  const row = buildAttendee(studentId, 'makeup', homeClass)
  // 临时跟的是当前这节课的教案消课
  const enrollment = enrollmentsService.pickEnrollmentForPackage(studentId, lesson.packageId) ||
    enrollmentsService.pickEnrollmentForPackage(studentId, homeClass.packageId)
  if (enrollment) row.enrollmentId = enrollment.id

  lesson.attendees.push(row)
  saveLessons(list)
  return { ok: true, lesson, attendee: row }
}

function maskPhone(phone) {
  const p = String(phone || '')
  if (p.length < 7) return p || '-'
  return `${p.slice(0, 3)}****${p.slice(-4)}`
}

function getMakeupCandidates(lessonId) {
  const lesson = getLessonById(lessonId)
  if (!lesson) return []
  const inLesson = new Set(lesson.attendees.map((a) => a.studentId))
  // 优先：近期旷课学员；其次其他有报读的学员
  const absentIds = new Set()
  getAllLessons().forEach((l) => {
    l.attendees.forEach((a) => {
      if (a.absent) absentIds.add(a.studentId)
    })
  })

  return studentsService
    .getAllStudents()
    .filter((s) => !inLesson.has(s.id))
    .map((s) => {
      const home = getClasses().find((c) => (c.studentIds || []).indexOf(s.id) >= 0)
      const isAbsent = absentIds.has(s.id)
      return {
        ...s,
        homeClassName: home ? home.name : '未分班',
        homeClassId: home ? home.id : '',
        reason: isAbsent ? '近期旷课，可补课' : '临时插班',
        reasonType: isAbsent ? 'absent' : 'normal',
        phoneMask: maskPhone(s.parentPhone),
        avatarText: (s.studentName || '学').slice(0, 1)
      }
    })
    .sort((a, b) => {
      if (a.reasonType === b.reasonType) return 0
      return a.reasonType === 'absent' ? -1 : 1
    })
}

/**
 * 姓名 / 家长手机号 / 家长姓名 模糊查询
 * 无关键词时返回近期旷课建议，避免一上来堆全量名单
 */
function searchMakeupCandidates(lessonId, keyword) {
  const all = getMakeupCandidates(lessonId)
  const kw = String(keyword || '')
    .trim()
    .toLowerCase()
  const suggestions = all.filter((s) => s.reasonType === 'absent').slice(0, 8)

  if (!kw) {
    return {
      mode: 'suggest',
      list: suggestions,
      total: all.length
    }
  }

  const list = all.filter((s) => {
    const name = String(s.studentName || '').toLowerCase()
    const phone = String(s.parentPhone || '')
    const parent = String(s.parentName || '').toLowerCase()
    return name.indexOf(kw) >= 0 || phone.indexOf(kw) >= 0 || parent.indexOf(kw) >= 0
  })

  return {
    mode: 'search',
    list,
    total: all.length
  }
}

function finishLesson(lessonId) {
  const list = getAllLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  lesson.status = 'finished'
  saveLessons(list)
  return { ok: true, lesson }
}

function rateByStudent(lessonId, studentId, score, comment) {
  const list = getAllLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  const attendee = lesson.attendees.find((a) => a.studentId === studentId)
  if (!attendee || attendee.absent) return { ok: false, message: '不在本课名单' }
  attendee.studentRated = true
  attendee.studentRating = { score: Number(score) || 5, comment: comment || '' }
  // 学生评老师可选，不影响消课
  saveLessons(list)
  return { ok: true, lesson, consumed: !!attendee.consumed, message: '感谢评价' }
}

function rateByTeacher(lessonId, studentId, score, comment) {
  const list = getAllLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  const attendee = lesson.attendees.find((a) => a.studentId === studentId)
  if (!attendee || attendee.absent) return { ok: false, message: '不在本课名单' }
  attendee.teacherRated = true
  attendee.teacherRating = { score: Number(score) || 5, comment: comment || '' }
  const consumeResult = tryConsumeAttendee(attendee)
  saveLessons(list)
  return { ok: true, lesson, consumed: consumeResult.consumed, message: consumeResult.message }
}

function tryConsumeAttendee(attendee) {
  if (attendee.consumed) return { consumed: true, message: '已消课' }
  // 消课只依赖老师评学生；学生评老师可选
  if (!attendee.teacherRated) {
    return { consumed: false, message: '老师评价学生后消 1 节课' }
  }
  if (!attendee.enrollmentId) {
    return { consumed: false, message: '未关联报读，无法消课' }
  }
  const result = enrollmentsService.consumeOneLesson(attendee.enrollmentId)
  if (!result.ok) return { consumed: false, message: result.message }
  attendee.consumed = true
  return { consumed: true, message: '已消 1 节课' }
}

function markAbsent(lessonId, studentId, absent) {
  const list = getAllLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  const attendee = lesson.attendees.find((a) => a.studentId === studentId)
  if (!attendee) return { ok: false, message: '学员不在名单' }
  if (attendee.type === 'makeup') return { ok: false, message: '临时插班学员请直接移除逻辑（演示未做移除）' }
  attendee.absent = !!absent
  saveLessons(list)
  return { ok: true, lesson }
}

module.exports = {
  getClasses,
  getClassById,
  getAllLessons,
  getLessonById,
  getLessonsByDate,
  getLessonDatesForStudent,
  getLessonDatesForTeacher,
  getLessonDateMarksForStudent,
  getLessonDateMarksForTeacher,
  getStudentLessonsByDate,
  getTeacherLessonsByDate,
  addTempMakeupStudent,
  getMakeupCandidates,
  searchMakeupCandidates,
  maskPhone,
  finishLesson,
  rateByStudent,
  rateByTeacher,
  markAbsent,
  ensureClasses: ensureClasses,
  ensureLessons: ensureLessons
}
