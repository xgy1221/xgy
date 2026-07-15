const { todayKey, addDays } = require('../utils/date')
const enrollmentsService = require('./enrollments')
const studentsService = require('./students')

const CLASS_KEY = 'xgy_classes'
const LESSON_KEY = 'xgy_lessons'

const SEED_CLASSES = [
  {
    id: 'class_xuequ_math_a',
    orgId: 'org_xuequ',
    name: '四年级数学 A 班',
    teacherId: 'u_teacher',
    teacherName: '李老师',
    packageId: 'pkg_xuequ_math_48',
    packageName: '小学数学思维提升',
    room: 'A203',
    studentIds: ['stu_xuequ_yn', 'stu_xuequ_lz']
  },
  {
    id: 'class_xuequ_write_b',
    orgId: 'org_xuequ',
    name: '一年级书写 B 班',
    teacherId: 'u_teacher',
    teacherName: '李老师',
    packageId: 'pkg_xuequ_write_16',
    packageName: '硬笔书写课',
    room: 'C102',
    studentIds: ['stu_xuequ_yr', 'stu_xuequ_cs']
  },
  {
    id: 'class_qihang_en_a',
    orgId: 'org_qihang',
    name: '河东英语 A 班',
    teacherId: 'u_qh_teacher',
    teacherName: '韩老师',
    packageId: 'pkg_qihang_en_36',
    packageName: '少儿英语进阶营',
    room: 'E201',
    studentIds: ['stu_qihang_yn', 'stu_qihang_ly']
  }
]

function buildAttendee(studentId, type, homeClass) {
  const stu = studentsService.getStudentById(studentId) || {}
  const enrollment = enrollmentsService.pickEnrollmentForPackage(studentId, homeClass.packageId)
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

function stampOrg(lesson, cls) {
  return { ...lesson, orgId: cls.orgId || lesson.orgId }
}

function buildSeedLessons() {
  studentsService.getAllStudents()
  enrollmentsService.getAllEnrollments()
  const today = todayKey()
  const classA = SEED_CLASSES[0]
  const classB = SEED_CLASSES[1]
  const classQ = SEED_CLASSES[2]

  const lessonTodayA = stampOrg(
    {
      id: 'les_xuequ_today_a',
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
      status: 'ongoing',
      attendees: classA.studentIds.map((id) => buildAttendee(id, 'regular', classA))
    },
    classA
  )

  const lessonTodayB = stampOrg(
    {
      id: 'les_xuequ_today_b',
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
      attendees: classB.studentIds.map((id) => buildAttendee(id, 'regular', classB))
    },
    classB
  )

  const lessonTomorrowA = stampOrg(
    {
      id: 'les_xuequ_tmr_a',
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
    },
    classA
  )

  const lessonYesterdayB = stampOrg(
    {
      id: 'les_xuequ_yday_b',
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
        if (id === 'stu_xuequ_yr') {
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
    },
    classB
  )

  const lessonWeekA = stampOrg(
    {
      id: 'les_xuequ_week_a',
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
    },
    classA
  )

  const lessonQihangToday = stampOrg(
    {
      id: 'les_qihang_today',
      date: today,
      startTime: '18:00',
      endTime: '19:30',
      classId: classQ.id,
      className: classQ.name,
      teacherId: classQ.teacherId,
      teacherName: classQ.teacherName,
      packageId: classQ.packageId,
      packageName: classQ.packageName,
      room: classQ.room,
      status: 'upcoming',
      attendees: classQ.studentIds.map((id) => buildAttendee(id, 'regular', classQ))
    },
    classQ
  )

  return [lessonYesterdayB, lessonTodayB, lessonTodayA, lessonTomorrowA, lessonWeekA, lessonQihangToday]
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

function getClasses(orgId) {
  const list = ensureClasses().slice()
  if (!orgId) return list
  return list.filter((c) => c.orgId === orgId)
}

function getClassById(id) {
  return ensureClasses().find((c) => c.id === id) || null
}

function getAllLessons(orgId) {
  const list = ensureLessons().slice()
  if (!orgId) return list
  return list.filter((l) => l.orgId === orgId)
}

function saveLessons(list) {
  wx.setStorageSync(LESSON_KEY, list)
}

function getLessonById(id) {
  return ensureLessons().find((l) => l.id === id) || null
}

function getLessonsByDate(date, orgId) {
  return getAllLessons(orgId).filter((l) => l.date === date)
}

function getLessonDatesForStudent(studentId) {
  return getLessonDateMarksForStudent(studentId).map((m) => m.date)
}

function getLessonDatesForTeacher(orgId) {
  return getLessonDateMarksForTeacher(orgId).map((m) => m.date)
}

function collectDateMarks(matchLesson, orgId) {
  const map = {}
  getAllLessons(orgId).forEach((l) => {
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

function getLessonDateMarksForTeacher(orgId) {
  return collectDateMarks(() => true, orgId)
}

function getStudentLessonsByDate(studentId, date) {
  return getAllLessons().filter(
    (l) =>
      l.date === date && l.attendees.some((a) => a.studentId === studentId && !a.absent)
  )
}

function getTeacherLessonsByDate(date, orgId) {
  return getAllLessons(orgId).filter((l) => l.date === date)
}

function addTempMakeupStudent(lessonId, studentId) {
  const list = ensureLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  if (lesson.status === 'finished') return { ok: false, message: '本课已结束' }
  if (lesson.attendees.some((a) => a.studentId === studentId)) {
    return { ok: false, message: '该学员已在本课名单中' }
  }

  const student = studentsService.getStudentById(studentId)
  if (!student) return { ok: false, message: '学员不存在' }
  if (student.orgId !== lesson.orgId) {
    return { ok: false, message: '不能跨机构临时插班' }
  }

  const homeClass =
    getClasses(lesson.orgId).find((c) => (c.studentIds || []).indexOf(studentId) >= 0) ||
    getClassById(lesson.classId)
  if (!homeClass) return { ok: false, message: '找不到学员原班' }

  const row = buildAttendee(studentId, 'makeup', homeClass)
  const enrollment =
    enrollmentsService.pickEnrollmentForPackage(studentId, lesson.packageId) ||
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
  const absentIds = new Set()
  getAllLessons(lesson.orgId).forEach((l) => {
    l.attendees.forEach((a) => {
      if (a.absent) absentIds.add(a.studentId)
    })
  })

  return studentsService
    .listStudentsByOrg(lesson.orgId)
    .filter((s) => !inLesson.has(s.id))
    .map((s) => {
      const home = getClasses(lesson.orgId).find((c) => (c.studentIds || []).indexOf(s.id) >= 0)
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
  const list = ensureLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  lesson.status = 'finished'
  saveLessons(list)
  return { ok: true, lesson }
}

function rateByStudent(lessonId, studentId, score, comment) {
  const list = ensureLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  const attendee = lesson.attendees.find((a) => a.studentId === studentId)
  if (!attendee || attendee.absent) return { ok: false, message: '不在本课名单' }
  attendee.studentRated = true
  attendee.studentRating = { score: Number(score) || 5, comment: comment || '' }
  saveLessons(list)
  return { ok: true, lesson, consumed: !!attendee.consumed, message: '感谢评价' }
}

function rateByTeacher(lessonId, studentId, score, comment) {
  const list = ensureLessons()
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
  const list = ensureLessons()
  const lesson = list.find((l) => l.id === lessonId)
  if (!lesson) return { ok: false, message: '课次不存在' }
  const attendee = lesson.attendees.find((a) => a.studentId === studentId)
  if (!attendee) return { ok: false, message: '学员不在名单' }
  if (attendee.type === 'makeup') {
    return { ok: false, message: '临时插班学员请直接移除逻辑（演示未做移除）' }
  }
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
  ensureClasses,
  ensureLessons
}
