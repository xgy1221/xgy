const { ROLES } = require('../utils/constants')
const studentsService = require('./students')
const parentsService = require('./parents')

/** 演示账号：一个手机号可绑定多个角色，用于体验权限切换 */

const DEMO_USERS = [
  {
    id: 'u_parent',
    name: '王女士',
    phone: '13800000001',
    avatarText: '王',
    roles: [ROLES.STUDENT]
  },
  {
    id: 'u_teacher',
    name: '李老师',
    phone: '13800000002',
    avatarText: '李',
    roles: [ROLES.TEACHER],
    title: '数学主讲'
  },
  {
    id: 'u_academic',
    name: '赵教务',
    phone: '13800000003',
    avatarText: '赵',
    roles: [ROLES.ACADEMIC],
    campus: '城南校区'
  },
  {
    id: 'u_partner',
    name: '陈合伙人',
    phone: '13800000004',
    avatarText: '陈',
    roles: [ROLES.PARTNER],
    shareRatio: '15%'
  },
  {
    id: 'u_admin',
    name: '系统管理员',
    phone: '13800000005',
    avatarText: '管',
    roles: [ROLES.ADMIN]
  },
  {
    id: 'u_multi',
    name: '周总',
    phone: '13800000000',
    avatarText: '周',
    roles: [ROLES.PARTNER, ROLES.ADMIN, ROLES.ACADEMIC],
    shareRatio: '30%',
    campus: '总部'
  }
]

const STUDENT_SCHEDULE = {
  stu_yn: [
    { id: 's1', date: '今天', time: '16:00-17:30', course: '小学数学思维提升', room: 'A203', teacher: '李老师' },
    { id: 's2', date: '明天', time: '18:00-19:30', course: '英语阅读加油站', room: 'B105', teacher: '周老师' },
    { id: 's3', date: '周六', time: '10:00-11:30', course: '小学数学思维提升', room: 'A203', teacher: '李老师' }
  ],
  stu_yr: [
    { id: 's4', date: '今天', time: '15:00-16:00', course: '硬笔书写课', room: 'C102', teacher: '沈老师' },
    { id: 's5', date: '周日', time: '10:00-11:00', course: '绘本阅读课', room: 'B106', teacher: '周老师' }
  ]
}

const DEFAULT_SCHEDULE = [
  { id: 's0', date: '待排课', time: '-', course: '暂无课次', room: '-', teacher: '-' }
]

const TEACHER_CLASSES = [
  { id: 'cl1', name: '四年级数学 A 班', students: 18, nextLesson: '今天 16:00', room: 'A203' },
  { id: 'cl2', name: '五年级数学冲刺班', students: 12, nextLesson: '周五 19:00', room: 'A301' }
]

const TEACHER_STUDENTS = [
  { id: 'st1', name: '王一诺', className: '四年级数学 A 班', attendRate: '96%', remark: '课堂专注' },
  { id: 'st2', name: '刘梓轩', className: '四年级数学 A 班', attendRate: '88%', remark: '作业需督促' },
  { id: 'st3', name: '陈思琪', className: '五年级数学冲刺班', attendRate: '100%', remark: '进步明显' }
]

const ACADEMIC_ENROLLS = [
  { id: 'e1', student: '王一诺', course: '小学数学思维提升', status: '待排课', amount: '¥3,680' },
  { id: 'e2', student: '张沐阳', course: '编程启蒙 L1', status: '待缴费', amount: '¥4,200' },
  { id: 'e3', student: '苏晴', course: '英语阅读加油站', status: '已入学', amount: '¥3,280' }
]

const PARTNER_STATS = {
  monthSales: '128,600',
  monthNewStudents: 26,
  monthRefund: '3,200',
  shareAmount: '19,290'
}

const PARTNER_TEAM = [
  { id: 'p1', name: '陈合伙人', role: '合伙人', students: 42, sales: '68,200' },
  { id: 'p2', name: '何顾问', role: '顾问', students: 19, sales: '31,400' },
  { id: 'p3', name: '林顾问', role: '顾问', students: 15, sales: '29,000' }
]

const ADMIN_USERS = [
  { id: 'au1', name: '周总', phone: '13800000000', roles: ['合伙人', '管理员', '教务'], status: '正常' },
  { id: 'au2', name: '李老师', phone: '13800000002', roles: ['授课老师'], status: '正常' },
  { id: 'au3', name: '赵教务', phone: '13800000003', roles: ['教务'], status: '正常' },
  { id: 'au4', name: '王女士', phone: '13800000001', roles: ['学生/家长'], status: '正常' }
]

const ORG_NODES = [
  { id: 'o1', name: '学管云总校', type: '总部', children: 2 },
  { id: 'o2', name: '城南校区', type: '校区', children: 5 },
  { id: 'o3', name: '高新校区', type: '校区', children: 3 }
]

function findUserByPhone(phone) {
  return DEMO_USERS.find((u) => u.phone === phone) || null
}

function buildParentUser(phone, linkedStudents, profile) {
  const parentName =
    (profile && profile.parentName) ||
    (linkedStudents[0] && linkedStudents[0].parentName) ||
    '家长'
  return {
    id: `u_parent_${phone}`,
    name: parentName,
    phone,
    avatarText: parentName.slice(0, 1),
    roles: [ROLES.STUDENT]
  }
}

/**
 * 登录规则：
 * 1) 员工演示账号按角色登录
 * 2) 任意 11 位手机号均可作为家长登录；未录过则自动建档
 * 3) 家长若尚无学员，标记 needsOnboarding
 */
function loginByPhone(phone) {
  if (!/^1\d{10}$/.test(phone || '')) {
    return { ok: false, message: '请输入正确的 11 位手机号' }
  }

  studentsService.getAllStudents()
  const staff = findUserByPhone(phone)
  const linkedStudents = studentsService.getStudentsByPhone(phone)

  if (staff && !staff.roles.includes(ROLES.STUDENT)) {
    return {
      ok: true,
      session: {
        token: `demo_${staff.id}_${Date.now()}`,
        user: staff,
        currentRole: staff.roles.length === 1 ? staff.roles[0] : null,
        currentStudentId: null,
        needsOnboarding: false,
        loggedAt: Date.now()
      }
    }
  }

  const knownBefore =
    parentsService.isPhoneKnown(phone) || linkedStudents.length > 0 || !!(staff && staff.roles.includes(ROLES.STUDENT))

  const profile = parentsService.ensureParentAccess(
    phone,
    (staff && staff.name) || (linkedStudents[0] && linkedStudents[0].parentName) || ''
  )

  const user = staff && staff.roles.includes(ROLES.STUDENT)
    ? { ...staff, name: profile.parentName || staff.name }
    : buildParentUser(phone, linkedStudents, profile)

  const needsOnboarding = linkedStudents.length === 0
  const currentStudentId = linkedStudents.length ? linkedStudents[0].id : null

  return {
    ok: true,
    session: {
      token: `demo_${user.id}_${Date.now()}`,
      user,
      currentRole: user.roles.length === 1 ? user.roles[0] : null,
      currentStudentId,
      needsOnboarding,
      isNewParent: !knownBefore,
      loggedAt: Date.now()
    }
  }
}

function getScheduleForStudent(studentId) {
  return STUDENT_SCHEDULE[studentId] || DEFAULT_SCHEDULE
}

module.exports = {
  DEMO_USERS,
  STUDENT_SCHEDULE,
  TEACHER_CLASSES,
  TEACHER_STUDENTS,
  ACADEMIC_ENROLLS,
  PARTNER_STATS,
  PARTNER_TEAM,
  ADMIN_USERS,
  ORG_NODES,
  findUserByPhone,
  loginByPhone,
  getScheduleForStudent
}
