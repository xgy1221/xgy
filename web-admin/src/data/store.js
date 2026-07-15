import { getUser } from '../auth/roles'

const KEY = 'xgy_web_admin_v3'

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

function money(n) {
  return Math.round(Number(n) || 0)
}

const ORG_XUEQU = 'org_xuequ'
const ORG_QIHANG = 'org_qihang'

const SEED = {
  orgs: [
    { id: ORG_XUEQU, name: '学趣思维' },
    { id: ORG_QIHANG, name: '启航英语' }
  ],
  campuses: [
    { id: 'camp_south', name: '城南校区', orgId: ORG_XUEQU },
    { id: 'camp_hi', name: '高新校区', orgId: ORG_XUEQU },
    { id: 'camp_hq', name: '总部', orgId: ORG_XUEQU },
    { id: 'camp_hedong', name: '河东校区', orgId: ORG_QIHANG }
  ],
  teachers: [
    {
      id: 't_li',
      name: '李老师',
      phone: '13800000002',
      title: '数学主讲',
      campus: '城南校区',
      status: '在职',
      subjects: '数学',
      orgId: ORG_XUEQU
    },
    {
      id: 't_zhou',
      name: '周老师',
      phone: '13800000006',
      title: '英语主讲',
      campus: '城南校区',
      status: '在职',
      subjects: '英语',
      orgId: ORG_XUEQU
    },
    {
      id: 't_shen',
      name: '沈老师',
      phone: '13800000007',
      title: '书法主讲',
      campus: '高新校区',
      status: '在职',
      subjects: '书法',
      orgId: ORG_XUEQU
    },
    {
      id: 't_han',
      name: '韩老师',
      phone: '13800000041',
      title: '英语主讲',
      campus: '河东校区',
      status: '在职',
      subjects: '英语',
      orgId: ORG_QIHANG
    }
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
      outline: '认识规律 / 速算技巧 / 应用题建模 / 阶段测评',
      orgId: ORG_XUEQU
    },
    {
      id: 'pkg_en_24',
      name: '英语阅读加油站',
      subject: '英语',
      grade: '小学2-4年级',
      lessonCount: 24,
      price: 3280,
      status: '上架',
      outline: '绘本跟读 / 词汇闯关 / 短文理解',
      orgId: ORG_XUEQU
    },
    {
      id: 'pkg_write_16',
      name: '硬笔书写课',
      subject: '书法',
      grade: '小学1-3年级',
      lessonCount: 16,
      price: 1680,
      status: '上架',
      outline: '握笔姿势 / 基本笔画 / 独体字',
      orgId: ORG_XUEQU
    },
    {
      id: 'pkg_code_32',
      name: '编程启蒙 L1',
      subject: '编程',
      grade: '小学3-6年级',
      lessonCount: 32,
      price: 4200,
      status: '上架',
      outline: 'Scratch 入门 / 动画故事 / 简单游戏',
      orgId: ORG_XUEQU
    },
    {
      id: 'pkg_qh_adv_36',
      name: '少儿英语进阶营',
      subject: '英语',
      grade: '小学1-6年级',
      lessonCount: 36,
      price: 4580,
      status: '上架',
      outline: '词汇拓展 / 阅读理解 / 语法进阶',
      orgId: ORG_QIHANG
    },
    {
      id: 'pkg_qh_speak_20',
      name: '口语角',
      subject: '英语',
      grade: '小学2-6年级',
      lessonCount: 20,
      price: 1980,
      status: '上架',
      outline: '情景对话 / 发音纠正 / 主题表达',
      orgId: ORG_QIHANG
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
      remark: '',
      status: '在读',
      orgId: ORG_XUEQU
    },
    {
      id: 'stu_yr',
      studentName: '王一然',
      parentPhone: '13800000001',
      parentName: '王女士',
      grade: '小学一年级',
      campus: '城南校区',
      remark: '同家庭二孩',
      status: '在读',
      orgId: ORG_XUEQU
    },
    {
      id: 'stu_lz',
      studentName: '刘梓轩',
      parentPhone: '13800000011',
      parentName: '刘先生',
      grade: '小学四年级',
      campus: '城南校区',
      remark: '',
      status: '在读',
      orgId: ORG_XUEQU
    },
    {
      id: 'stu_cs',
      studentName: '陈思琪',
      parentPhone: '13800000012',
      parentName: '陈女士',
      grade: '小学五年级',
      campus: '高新校区',
      remark: '',
      status: '在读',
      orgId: ORG_XUEQU
    },
    {
      id: 'stu_qh_yn',
      studentName: '王一诺',
      parentPhone: '13800000001',
      parentName: '王女士',
      grade: '小学三年级',
      campus: '河东校区',
      remark: '',
      status: '在读',
      orgId: ORG_QIHANG
    },
    {
      id: 'stu_qh_lyt',
      studentName: '林雨桐',
      parentPhone: '13800000051',
      parentName: '林女士',
      grade: '小学四年级',
      campus: '河东校区',
      remark: '',
      status: '在读',
      orgId: ORG_QIHANG
    }
  ],
  classes: [
    {
      id: 'class_math_a',
      name: '四年级数学 A 班',
      packageId: 'pkg_math_48',
      teacherId: 't_li',
      campus: '城南校区',
      room: 'A203',
      studentIds: ['stu_yn', 'stu_lz'],
      status: '开班中',
      orgId: ORG_XUEQU
    },
    {
      id: 'class_write_b',
      name: '一年级书写 B 班',
      packageId: 'pkg_write_16',
      teacherId: 't_shen',
      campus: '高新校区',
      room: 'C102',
      studentIds: ['stu_yr', 'stu_cs'],
      status: '开班中',
      orgId: ORG_XUEQU
    },
    {
      id: 'class_en_c',
      name: '英语阅读 C 班',
      packageId: 'pkg_en_24',
      teacherId: 't_zhou',
      campus: '城南校区',
      room: 'B105',
      studentIds: ['stu_yn'],
      status: '开班中',
      orgId: ORG_XUEQU
    },
    {
      id: 'class_qh_adv',
      name: '少儿英语进阶 A 班',
      packageId: 'pkg_qh_adv_36',
      teacherId: 't_han',
      campus: '河东校区',
      room: '101',
      studentIds: ['stu_qh_yn', 'stu_qh_lyt'],
      status: '开班中',
      orgId: ORG_QIHANG
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
      status: 'upcoming',
      orgId: ORG_XUEQU
    },
    {
      id: 'les_qh_today',
      date: todayKey(),
      startTime: '15:00',
      endTime: '16:30',
      classId: 'class_qh_adv',
      teacherId: 't_han',
      room: '101',
      status: 'upcoming',
      orgId: ORG_QIHANG
    }
  ],
  /** 报名订单 / 财务流水（教培常见：实收、欠费、退费、渠道） */
  orders: [
    {
      id: 'ord_1',
      studentId: 'stu_yn',
      packageId: 'pkg_math_48',
      campus: '城南校区',
      amount: 3680,
      paidAmount: 3680,
      refundAmount: 0,
      status: '已缴费',
      channel: '地推',
      partnerId: 'p_chen',
      createdAt: '2026-06-08',
      orgId: ORG_XUEQU
    },
    {
      id: 'ord_2',
      studentId: 'stu_yn',
      packageId: 'pkg_en_24',
      campus: '城南校区',
      amount: 3280,
      paidAmount: 2000,
      refundAmount: 0,
      status: '部分缴费',
      channel: '老带新',
      partnerId: 'p_chen',
      createdAt: '2026-06-20',
      orgId: ORG_XUEQU
    },
    {
      id: 'ord_3',
      studentId: 'stu_lz',
      packageId: 'pkg_math_48',
      campus: '城南校区',
      amount: 3680,
      paidAmount: 3680,
      refundAmount: 500,
      status: '部分退费',
      channel: '自然到访',
      partnerId: 'p_chen',
      createdAt: '2026-05-12',
      orgId: ORG_XUEQU
    },
    {
      id: 'ord_4',
      studentId: 'stu_cs',
      packageId: 'pkg_write_16',
      campus: '高新校区',
      amount: 1680,
      paidAmount: 1680,
      refundAmount: 0,
      status: '已缴费',
      channel: '地推',
      partnerId: 'p_other',
      createdAt: '2026-07-01',
      orgId: ORG_XUEQU
    },
    {
      id: 'ord_5',
      studentId: 'stu_yr',
      packageId: 'pkg_write_16',
      campus: '城南校区',
      amount: 1680,
      paidAmount: 1680,
      refundAmount: 0,
      status: '已缴费',
      channel: '老带新',
      partnerId: 'p_chen',
      createdAt: '2026-07-03',
      orgId: ORG_XUEQU
    },
    {
      id: 'ord_6',
      studentId: 'stu_lz',
      packageId: 'pkg_code_32',
      campus: '城南校区',
      amount: 4200,
      paidAmount: 0,
      refundAmount: 0,
      status: '待缴费',
      channel: '销售跟进',
      partnerId: 'p_chen',
      createdAt: '2026-07-10',
      orgId: ORG_XUEQU
    },
    {
      id: 'ord_qh_1',
      studentId: 'stu_qh_yn',
      packageId: 'pkg_qh_adv_36',
      campus: '河东校区',
      amount: 4580,
      paidAmount: 4580,
      refundAmount: 0,
      status: '已缴费',
      channel: '自然到访',
      partnerId: '',
      createdAt: '2026-07-05',
      orgId: ORG_QIHANG
    },
    {
      id: 'ord_qh_2',
      studentId: 'stu_qh_lyt',
      packageId: 'pkg_qh_speak_20',
      campus: '河东校区',
      amount: 1980,
      paidAmount: 1000,
      refundAmount: 0,
      status: '部分缴费',
      channel: '地推',
      partnerId: '',
      createdAt: '2026-07-12',
      orgId: ORG_QIHANG
    }
  ],
  staffAccounts: [
    {
      id: 'u_aca',
      name: '赵教务',
      phone: '13800000003',
      roles: ['教务'],
      campus: '城南校区',
      status: '正常',
      orgId: ORG_XUEQU
    },
    {
      id: 'u_partner',
      name: '陈合伙人',
      phone: '13800000004',
      roles: ['合伙人', '教务', '老师'],
      campus: '城南校区',
      status: '正常',
      orgId: ORG_XUEQU
    },
    {
      id: 'u_admin',
      name: '周总',
      phone: '13800000000',
      roles: ['管理员', '合伙人'],
      campus: '总部',
      status: '正常',
      orgId: ORG_XUEQU
    },
    {
      id: 'u_tea',
      name: '李老师',
      phone: '13800000002',
      roles: ['老师'],
      campus: '城南校区',
      status: '正常',
      orgId: ORG_XUEQU
    },
    {
      id: 'u_qh_aca',
      name: '启航教务',
      phone: '13800000040',
      roles: ['教务'],
      campus: '河东校区',
      status: '正常',
      orgId: ORG_QIHANG
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

function scopeByOrg(list, user) {
  if (!user?.orgId) return []
  return list.filter((i) => i.orgId === user.orgId)
}

function scopeByCampus(list, user, field = 'campus') {
  if (!user || user.role === 'admin' || user.role === 'academic') return list
  if (user.role === 'partner') return list.filter((i) => i[field] === user.campus)
  return list
}

function resolveOrgId(explicit) {
  if (explicit) return explicit
  return getUser()?.orgId || ''
}

export function listOrgs() {
  return getDb().orgs || []
}

export function getOrgName(id) {
  const org = (getDb().orgs || []).find((o) => o.id === id)
  return org?.name || '-'
}

export function listCampuses(user) {
  return scopeByOrg(getDb().campuses, user)
}

export function listTeachers(user) {
  return scopeByCampus(scopeByOrg(getDb().teachers, user), user)
}

export function upsertTeacher(teacher) {
  const db = getDb()
  const orgId = resolveOrgId(teacher.orgId)
  if (teacher.id) {
    const idx = db.teachers.findIndex((t) => t.id === teacher.id)
    if (idx >= 0) {
      const existing = db.teachers[idx]
      if (existing.orgId && orgId && existing.orgId !== orgId) return db.teachers
      db.teachers[idx] = { ...existing, ...teacher, orgId: existing.orgId || orgId }
    }
  } else {
    db.teachers.unshift({
      ...teacher,
      id: uid('t'),
      status: teacher.status || '在职',
      orgId
    })
  }
  save(db)
  return db.teachers
}

export function listPackages(user) {
  return scopeByOrg(getDb().packages, user)
}

export function upsertPackage(pkg) {
  const db = getDb()
  const orgId = resolveOrgId(pkg.orgId)
  if (pkg.id) {
    const idx = db.packages.findIndex((p) => p.id === pkg.id)
    if (idx >= 0) {
      const existing = db.packages[idx]
      if (existing.orgId && orgId && existing.orgId !== orgId) return db.packages
      db.packages[idx] = { ...existing, ...pkg, orgId: existing.orgId || orgId }
    } else {
      db.packages.unshift({ ...pkg, id: pkg.id, orgId })
    }
  } else {
    db.packages.unshift({
      ...pkg,
      id: uid('pkg'),
      status: pkg.status || '上架',
      orgId
    })
  }
  save(db)
  return db.packages
}

export function listStudents(user) {
  return scopeByCampus(scopeByOrg(getDb().students, user), user)
}

export function upsertStudent(stu) {
  const db = getDb()
  const orgId = resolveOrgId(stu.orgId)
  if (stu.id) {
    const idx = db.students.findIndex((s) => s.id === stu.id)
    if (idx >= 0) {
      const existing = db.students[idx]
      if (existing.orgId && orgId && existing.orgId !== orgId) return db.students
      db.students[idx] = { ...existing, ...stu, orgId: existing.orgId || orgId }
    }
  } else {
    db.students.unshift({ ...stu, id: uid('stu'), status: stu.status || '在读', orgId })
  }
  save(db)
  return db.students
}

export function listClasses(user) {
  const db = getDb()
  return scopeByCampus(scopeByOrg(db.classes, user), user).map((c) => enrichClass(c, db))
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
    packagePrice: pkg?.price || 0,
    teacherName: teacher?.name || '未分配',
    students,
    studentCount: students.length
  }
}

export function createClass(payload) {
  const db = getDb()
  const orgId = resolveOrgId(payload.orgId)
  if (!orgId) return { ok: false, message: '缺少机构信息' }

  const pkg = db.packages.find((p) => p.id === payload.packageId)
  if (payload.packageId && (!pkg || pkg.orgId !== orgId)) {
    return { ok: false, message: '教案不属于当前机构' }
  }
  const teacher = db.teachers.find((t) => t.id === payload.teacherId)
  if (payload.teacherId && (!teacher || teacher.orgId !== orgId)) {
    return { ok: false, message: '教师不属于当前机构' }
  }
  const studentIds = payload.studentIds || []
  for (const sid of studentIds) {
    const stu = db.students.find((s) => s.id === sid)
    if (!stu || stu.orgId !== orgId) {
      return { ok: false, message: '学员不属于当前机构' }
    }
  }

  const item = {
    id: uid('class'),
    name: payload.name,
    packageId: payload.packageId,
    teacherId: payload.teacherId,
    campus: payload.campus || '城南校区',
    room: payload.room || '',
    studentIds,
    status: '开班中',
    orgId
  }
  db.classes.unshift(item)
  save(db)
  return enrichClass(item, db)
}

export function addStudentToClass(classId, studentId) {
  const db = getDb()
  const cls = db.classes.find((c) => c.id === classId)
  if (!cls) return { ok: false, message: '班级不存在' }
  const stu = db.students.find((s) => s.id === studentId)
  if (!stu || (cls.orgId && stu.orgId !== cls.orgId)) {
    return { ok: false, message: '学员不属于当前机构' }
  }
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

export function listLessons(user) {
  const db = getDb()
  let list = scopeByOrg(db.lessons, user).map((l) => enrichLesson(l, db))
  if (user?.role === 'partner') list = list.filter((l) => l.campus === user.campus)
  return list
}

function enrichLesson(l, db) {
  const cls = db.classes.find((c) => c.id === l.classId)
  const teacher =
    db.teachers.find((t) => t.id === l.teacherId) ||
    (cls ? db.teachers.find((t) => t.id === cls.teacherId) : null)
  const pkg = cls ? db.packages.find((p) => p.id === cls.packageId) : null
  return {
    ...l,
    orgId: l.orgId || cls?.orgId,
    className: cls?.name || '未知班级',
    campus: cls?.campus || '-',
    teacherName: teacher?.name || '未分配',
    packageName: pkg?.name || '-',
    studentCount: cls?.studentIds?.length || 0
  }
}

export function getLessonsByDate(date, user) {
  return listLessons(user)
    .filter((l) => l.date === date)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
}

export function getLessonDateMarks(user) {
  const map = {}
  listLessons(user).forEach((l) => {
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
  const orgId = resolveOrgId(payload.orgId || cls.orgId)
  if (cls.orgId && orgId && cls.orgId !== orgId) {
    return { ok: false, message: '班级不属于当前机构' }
  }
  const item = {
    id: uid('les'),
    date: payload.date,
    startTime: payload.startTime || '16:00',
    endTime: payload.endTime || '17:30',
    classId: payload.classId,
    teacherId: payload.teacherId || cls.teacherId,
    room: payload.room || cls.room || '',
    status: 'upcoming',
    orgId: cls.orgId || orgId
  }
  db.lessons.unshift(item)
  save(db)
  return { ok: true, lesson: enrichLesson(item, db) }
}

function enrichOrder(o, db) {
  const stu = db.students.find((s) => s.id === o.studentId)
  const pkg = db.packages.find((p) => p.id === o.packageId)
  const receivable = money(o.amount - o.paidAmount)
  const net = money(o.paidAmount - o.refundAmount)
  return {
    ...o,
    studentName: stu?.studentName || '未知学员',
    parentPhone: stu?.parentPhone || '-',
    packageName: pkg?.name || '未知教案',
    lessonCount: pkg?.lessonCount || 0,
    receivable,
    net
  }
}

export function listOrders(user) {
  const db = getDb()
  let orders = scopeByOrg(db.orders, user).map((o) => enrichOrder(o, db))
  if (user?.role === 'partner') {
    orders = orders.filter((o) => o.campus === user.campus && o.partnerId === 'p_chen')
  }
  return orders.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
}

export function getFinanceSummary(user) {
  const orders = listOrders(user)
  const sales = orders.reduce((s, o) => s + money(o.amount), 0)
  const received = orders.reduce((s, o) => s + money(o.paidAmount), 0)
  const refund = orders.reduce((s, o) => s + money(o.refundAmount), 0)
  const receivable = orders.reduce((s, o) => s + money(o.receivable), 0)
  const net = received - refund
  const ratio = user?.shareRatio != null ? Number(user.shareRatio) : user?.role === 'partner' ? 0.15 : 0
  const share = money(net * ratio)

  const byCampus = {}
  orders.forEach((o) => {
    if (!byCampus[o.campus]) byCampus[o.campus] = { campus: o.campus, received: 0, refund: 0, net: 0 }
    byCampus[o.campus].received += money(o.paidAmount)
    byCampus[o.campus].refund += money(o.refundAmount)
    byCampus[o.campus].net += money(o.net)
  })

  const byPackage = {}
  orders.forEach((o) => {
    if (!byPackage[o.packageName]) {
      byPackage[o.packageName] = { name: o.packageName, count: 0, received: 0 }
    }
    byPackage[o.packageName].count += 1
    byPackage[o.packageName].received += money(o.paidAmount)
  })

  return {
    sales,
    received,
    refund,
    receivable,
    net,
    share,
    shareRatio: ratio,
    orderCount: orders.length,
    byCampus: Object.values(byCampus),
    byPackage: Object.values(byPackage),
    orders
  }
}

export function listStaffAccounts(user) {
  return scopeByOrg(getDb().staffAccounts, user)
}

export function getDashboardStats(user) {
  const students = listStudents(user)
  const teachers = listTeachers(user)
  const classes = listClasses(user)
  const packages = listPackages(user)
  const todayLessons = getLessonsByDate(todayKey(), user)
  const finance = user && (user.role === 'partner' || user.role === 'admin') ? getFinanceSummary(user) : null
  return {
    studentCount: students.length,
    teacherCount: teachers.length,
    classCount: classes.length,
    packageCount: packages.length,
    todayLessonCount: todayLessons.length,
    finance
  }
}

export function formatMoney(n) {
  return `¥${money(n).toLocaleString('zh-CN')}`
}
