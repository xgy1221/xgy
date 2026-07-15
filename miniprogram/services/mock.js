const { ROLES, ROLE_META } = require('../utils/constants')
const studentsService = require('./students')
const parentsService = require('./parents')
const orgs = require('./orgs')

/** 演示账号：员工绑定单一机构；家长手机号可跨多个机构有学员 */

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
    title: '数学主讲',
    orgId: 'org_xuequ',
    campus: '城南校区'
  },
  {
    id: 'u_academic',
    name: '赵教务',
    phone: '13800000003',
    avatarText: '赵',
    roles: [ROLES.ACADEMIC],
    orgId: 'org_xuequ',
    campus: '城南校区'
  },
  {
    id: 'u_partner',
    name: '陈合伙人',
    phone: '13800000004',
    avatarText: '陈',
    roles: [ROLES.PARTNER, ROLES.ACADEMIC, ROLES.TEACHER],
    shareRatio: '15%',
    orgId: 'org_xuequ',
    campus: '城南校区',
    title: '兼职主讲'
  },
  {
    id: 'u_admin',
    name: '学趣管理员',
    phone: '13800000005',
    avatarText: '管',
    roles: [ROLES.ADMIN],
    orgId: 'org_xuequ',
    campus: '总部'
  },
  {
    id: 'u_multi',
    name: '周总',
    phone: '13800000000',
    avatarText: '周',
    roles: [ROLES.PARTNER, ROLES.ADMIN, ROLES.ACADEMIC, ROLES.TEACHER],
    shareRatio: '30%',
    orgId: 'org_xuequ',
    campus: '总部',
    title: '教研统筹'
  },
  {
    id: 'u_qh_academic',
    name: '启航教务',
    phone: '13800000040',
    avatarText: '启',
    roles: [ROLES.ACADEMIC],
    orgId: 'org_qihang',
    campus: '河东校区'
  },
  {
    id: 'u_qh_teacher',
    name: '韩老师',
    phone: '13800000041',
    avatarText: '韩',
    roles: [ROLES.TEACHER],
    title: '英语主讲',
    orgId: 'org_qihang',
    campus: '河东校区'
  }
]

const STUDENT_SCHEDULE = {
  stu_xuequ_yn: [
    {
      id: 's1',
      date: '今天',
      time: '16:00-17:30',
      course: '小学数学思维提升',
      room: 'A203',
      teacher: '李老师'
    }
  ],
  stu_xuequ_yr: [
    {
      id: 's4',
      date: '今天',
      time: '15:00-16:00',
      course: '硬笔书写课',
      room: 'C102',
      teacher: '沈老师'
    }
  ],
  stu_qihang_yn: [
    {
      id: 's6',
      date: '今天',
      time: '18:00-19:30',
      course: '少儿英语进阶营',
      room: 'E201',
      teacher: '韩老师'
    }
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
  monthReceived: '112,400',
  monthNewStudents: 26,
  monthRefund: '3,200',
  shareAmount: '19,290',
  shareRatio: '15%',
  campus: '城南校区',
  pendingOrders: 3,
  topPackages: [
    { name: '小学数学思维提升', count: 12, amount: '44,160' },
    { name: '英语阅读加油站', count: 8, amount: '26,240' },
    { name: '硬笔书写课', count: 6, amount: '10,080' }
  ]
}

const PARTNER_TEAM = [
  { id: 'p1', name: '陈合伙人', role: '合伙人', students: 42, sales: '68,200' },
  { id: 'p2', name: '何顾问', role: '顾问', students: 19, sales: '31,400' },
  { id: 'p3', name: '林顾问', role: '顾问', students: 15, sales: '29,000' }
]

const ADMIN_STATS = {
  monthSales: '286,400',
  monthReceived: '251,200',
  monthRefund: '8,600',
  receivable: '35,200',
  studentCount: 186,
  teacherCount: 28,
  classCount: 42,
  todayLessons: 16,
  openCampuses: 2
}

function getAdminUserList(orgId) {
  return DEMO_USERS.filter((u) => !orgId || u.orgId === orgId || !u.orgId).map((u) => ({
    id: u.id,
    name: u.name,
    phone: u.phone,
    orgId: u.orgId || '',
    orgName: u.orgId ? orgs.getOrgName(u.orgId) : '跨机构家长',
    roles: (u.roles || []).map((r) => (ROLE_META[r] && ROLE_META[r].name) || r),
    status: '正常'
  }))
}

function getOrgNodes(orgId) {
  const org = orgs.getOrgById(orgId || 'org_xuequ')
  if (!org) return []
  return [
    { id: `${org.id}_hq`, name: org.name, type: '机构', children: (org.campuses || []).length },
    ...(org.campuses || []).map((name, idx) => ({
      id: `${org.id}_c_${idx}`,
      name,
      type: '校区',
      children: 0
    }))
  ]
}

const ORG_NODES = getOrgNodes('org_xuequ')

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
 * 1) 员工演示账号按角色登录；绑定单一 orgId
 * 2) 任意 11 位手机号均可作为家长登录；学员可跨多机构
 * 3) 家长若尚无学员，标记 needsOnboarding（需选择机构）
 */
function loginByPhone(phone) {
  if (!/^1\d{10}$/.test(phone || '')) {
    return { ok: false, message: '请输入正确的 11 位手机号' }
  }

  const auth = require('../utils/auth')
  studentsService.getAllStudents()
  const staff = findUserByPhone(phone)
  const linkedStudents = studentsService.getStudentsByPhone(phone)

  if (staff && !staff.roles.includes(ROLES.STUDENT)) {
    const currentRole = auth.resolvePreferredRole(staff)
    if (currentRole) auth.setLastRole(phone, currentRole)
    if (staff.orgId) auth.setLastOrg(phone, staff.orgId)
    return {
      ok: true,
      session: {
        token: `demo_${staff.id}_${Date.now()}`,
        user: staff,
        currentRole,
        currentStudentId: null,
        currentOrgId: staff.orgId || null,
        needsOnboarding: false,
        loggedAt: Date.now()
      }
    }
  }

  const knownBefore =
    parentsService.isPhoneKnown(phone) ||
    linkedStudents.length > 0 ||
    !!(staff && staff.roles.includes(ROLES.STUDENT))

  const profile = parentsService.ensureParentAccess(
    phone,
    (staff && staff.name) || (linkedStudents[0] && linkedStudents[0].parentName) || ''
  )

  const user =
    staff && staff.roles.includes(ROLES.STUDENT)
      ? { ...staff, name: profile.parentName || staff.name }
      : buildParentUser(phone, linkedStudents, profile)

  const needsOnboarding = linkedStudents.length === 0
  const currentStudentId = linkedStudents.length ? linkedStudents[0].id : null
  const currentOrgId = linkedStudents.length
    ? linkedStudents[0].orgId
    : auth.getLastOrg(phone) || null
  const currentRole = auth.resolvePreferredRole(user)
  if (currentRole) auth.setLastRole(phone, currentRole)
  if (currentOrgId) auth.setLastOrg(phone, currentOrgId)

  return {
    ok: true,
    session: {
      token: `demo_${user.id}_${Date.now()}`,
      user,
      currentRole,
      currentStudentId,
      currentOrgId,
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
  ADMIN_STATS,
  getAdminUserList,
  getOrgNodes,
  ORG_NODES,
  findUserByPhone,
  loginByPhone,
  getScheduleForStudent
}
