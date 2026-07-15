const KEY = 'xgy_web_admin_v1'

function pad(n) {
  return n < 10 ? `0${n}` : `${n}`
}

export function todayKey() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function uid(prefix) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
}

const SEED = {
  teachers: [
    { id: 't_li', name: '李老师', phone: '13800000002', title: '数学主讲' },
    { id: 't_zhou', name: '周老师', phone: '13800000006', title: '英语主讲' },
    { id: 't_shen', name: '沈老师', phone: '13800000007', title: '书法主讲' }
  ],
  packages: [
    {
      id: 'pkg_math_48',
      name: '小学数学思维提升',
      subject: '数学',
      grade: '小学3-5年级',
      lessonCount: 48,
      price: 3680,
      status: '上架',
      outline: '认识规律 / 速算技巧 / 应用题建模 / 阶段测评'
    },
    {
      id: 'pkg_en_24',
      name: '英语阅读加油站',
      subject: '英语',
      grade: '小学2-4年级',
      lessonCount: 24,
      price: 3280,
      status: '上架',
      outline: '绘本跟读 / 词汇闯关 / 短文理解'
    },
    {
      id: 'pkg_write_16',
      name: '硬笔书写课',
      subject: '书法',
      grade: '小学1-3年级',
      lessonCount: 16,
      price: 1680,
      status: '上架',
      outline: '握笔姿势 / 基本笔画 / 独体字'
    }
  ],
  students: [
    {
      id: 'stu_yn',
      studentName: '王一诺',
      parentPhone: '13800000001',
      parentName: '王女士',
      grade: '小学四年级',
      campus: '城南校区',
      remark: ''
    },
    {
      id: 'stu_yr',
      studentName: '王一然',
      parentPhone: '13800000001',
      parentName: '王女士',
      grade: '小学一年级',
      campus: '城南校区',
      remark: '同家庭二孩'
    },
    {
      id: 'stu_lz',
      studentName: '刘梓轩',
      parentPhone: '13800000011',
      parentName: '刘先生',
      grade: '小学四年级',
      campus: '城南校区',
      remark: ''
    },
    {
      id: 'stu_cs',
      studentName: '陈思琪',
      parentPhone: '13800000012',
      parentName: '陈女士',
      grade: '小学五年级',
      campus: '高新校区',
      remark: ''
    }
  ],
  classes: [
    {
      id: 'class_math_a',
      name: '四年级数学 A 班',
      packageId: 'pkg_math_48',
      teacherId: 't_li',
      room: 'A203',
      studentIds: ['stu_yn', 'stu_lz'],
      status: '开班中'
    },
    {
      id: 'class_write_b',
      name: '一年级书写 B 班',
      packageId: 'pkg_write_16',
      teacherId: 't_shen',
      room: 'C102',
      studentIds: ['stu_yr', 'stu_cs'],
      status: '开班中'
    }
  ],
  lessons: [
    {
      id: 'les_web_today',
      date: todayKey(),
      startTime: '16:00',
      endTime: '17:30',
      classId: 'class_math_a',
      teacherId: 't_li',
      room: 'A203',
      status: 'upcoming'
    }
  ]
}

function load() {
  const raw = localStorage.getItem(KEY)
  if (raw) {
    try {
      return JSON.parse(raw)
    } catch {
      // fallthrough
    }
  }
  localStorage.setItem(KEY, JSON.stringify(SEED))
  return structuredClone(SEED)
}

function save(db) {
  localStorage.setItem(KEY, JSON.stringify(db))
  return db
}

export function getDb() {
  return load()
}

export function resetDb() {
  localStorage.setItem(KEY, JSON.stringify(SEED))
  return structuredClone(SEED)
}

export function listTeachers() {
  return getDb().teachers
}

export function listPackages() {
  return getDb().packages
}

export function upsertPackage(pkg) {
  const db = getDb()
  if (pkg.id) {
    const idx = db.packages.findIndex((p) => p.id === pkg.id)
    if (idx >= 0) db.packages[idx] = { ...db.packages[idx], ...pkg }
    else db.packages.unshift({ ...pkg, id: pkg.id })
  } else {
    db.packages.unshift({
      ...pkg,
      id: uid('pkg'),
      status: pkg.status || '上架'
    })
  }
  save(db)
  return db.packages
}

export function listStudents() {
  return getDb().students
}

export function upsertStudent(stu) {
  const db = getDb()
  if (stu.id) {
    const idx = db.students.findIndex((s) => s.id === stu.id)
    if (idx >= 0) db.students[idx] = { ...db.students[idx], ...stu }
  } else {
    db.students.unshift({ ...stu, id: uid('stu') })
  }
  save(db)
  return db.students
}

export function listClasses() {
  const db = getDb()
  return db.classes.map((c) => enrichClass(c, db))
}

function enrichClass(c, db) {
  const pkg = db.packages.find((p) => p.id === c.packageId)
  const teacher = db.teachers.find((t) => t.id === c.teacherId)
  const students = (c.studentIds || [])
    .map((id) => db.students.find((s) => s.id === id))
    .filter(Boolean)
  return {
    ...c,
    packageName: pkg?.name || '未绑教案',
    lessonCount: pkg?.lessonCount || 0,
    teacherName: teacher?.name || '未分配',
    students,
    studentCount: students.length
  }
}

export function createClass(payload) {
  const db = getDb()
  const item = {
    id: uid('class'),
    name: payload.name,
    packageId: payload.packageId,
    teacherId: payload.teacherId,
    room: payload.room || '',
    studentIds: payload.studentIds || [],
    status: '开班中'
  }
  db.classes.unshift(item)
  save(db)
  return enrichClass(item, db)
}

export function updateClass(id, patch) {
  const db = getDb()
  const idx = db.classes.findIndex((c) => c.id === id)
  if (idx < 0) return null
  db.classes[idx] = { ...db.classes[idx], ...patch, id }
  save(db)
  return enrichClass(db.classes[idx], db)
}

export function addStudentToClass(classId, studentId) {
  const db = getDb()
  const cls = db.classes.find((c) => c.id === classId)
  if (!cls) return { ok: false, message: '班级不存在' }
  if ((cls.studentIds || []).includes(studentId)) {
    return { ok: false, message: '学员已在班中' }
  }
  cls.studentIds = [...(cls.studentIds || []), studentId]
  save(db)
  return { ok: true, class: enrichClass(cls, db) }
}

export function removeStudentFromClass(classId, studentId) {
  const db = getDb()
  const cls = db.classes.find((c) => c.id === classId)
  if (!cls) return { ok: false, message: '班级不存在' }
  cls.studentIds = (cls.studentIds || []).filter((id) => id !== studentId)
  save(db)
  return { ok: true, class: enrichClass(cls, db) }
}

export function listLessons() {
  const db = getDb()
  return db.lessons.map((l) => enrichLesson(l, db))
}

function enrichLesson(l, db) {
  const cls = db.classes.find((c) => c.id === l.classId)
  const teacher = db.teachers.find((t) => t.id === l.teacherId) ||
    (cls ? db.teachers.find((t) => t.id === cls.teacherId) : null)
  const pkg = cls ? db.packages.find((p) => p.id === cls.packageId) : null
  return {
    ...l,
    className: cls?.name || '未知班级',
    teacherName: teacher?.name || '未分配',
    packageName: pkg?.name || '-',
    studentCount: cls?.studentIds?.length || 0
  }
}

export function getLessonsByDate(date) {
  return listLessons()
    .filter((l) => l.date === date)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
}

export function getLessonDateMarks() {
  const map = {}
  listLessons().forEach((l) => {
    if (!map[l.date]) map[l.date] = { hasFinished: false, hasOpen: false }
    if (l.status === 'finished') map[l.date].hasFinished = true
    else map[l.date].hasOpen = true
  })
  return Object.keys(map).map((date) => {
    const row = map[date]
    let state = 'upcoming'
    if (row.hasFinished && row.hasOpen) state = 'mixed'
    else if (row.hasFinished) state = 'finished'
    return { date, state }
  })
}

export function createLesson(payload) {
  const db = getDb()
  const cls = db.classes.find((c) => c.id === payload.classId)
  if (!cls) return { ok: false, message: '请选择班级' }
  const item = {
    id: uid('les'),
    date: payload.date,
    startTime: payload.startTime || '16:00',
    endTime: payload.endTime || '17:30',
    classId: payload.classId,
    teacherId: payload.teacherId || cls.teacherId,
    room: payload.room || cls.room || '',
    status: 'upcoming'
  }
  db.lessons.unshift(item)
  save(db)
  return { ok: true, lesson: enrichLesson(item, db) }
}

export function getDashboardStats() {
  const db = getDb()
  const today = todayKey()
  return {
    packageCount: db.packages.length,
    studentCount: db.students.length,
    classCount: db.classes.length,
    todayLessonCount: db.lessons.filter((l) => l.date === today).length
  }
}
