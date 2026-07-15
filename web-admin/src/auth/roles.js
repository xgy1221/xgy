/** Web 端角色：教务 / 合伙人 / 管理员 */

export const WEB_ROLES = {
  ACADEMIC: 'academic',
  PARTNER: 'partner',
  ADMIN: 'admin'
}

export const ROLE_LABEL = {
  academic: '教务',
  partner: '合伙人',
  admin: '管理员'
}

/**
 * 市面教培后台常见分权：
 * - 教务：学员/老师/班级排课/教案，偏运营，一般不看完整财务
 * - 合伙人：看业绩与分成相关财务，学员/课程多为只读
 * - 管理员：全量配置 + 全量财务
 */
export const PERMISSIONS = {
  academic: {
    students: 'edit',
    teachers: 'edit',
    courses: 'edit',
    packages: 'edit',
    finance: 'none',
    users: 'none',
    campusScope: 'all'
  },
  partner: {
    students: 'view',
    teachers: 'view',
    courses: 'view',
    packages: 'view',
    finance: 'partner',
    users: 'none',
    campusScope: 'own'
  },
  admin: {
    students: 'edit',
    teachers: 'edit',
    courses: 'edit',
    packages: 'edit',
    finance: 'all',
    users: 'edit',
    campusScope: 'all'
  }
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem('xgy_web_user') || 'null')
  } catch {
    return null
  }
}

export function setUser(user) {
  localStorage.setItem('xgy_web_user', JSON.stringify(user))
}

export function clearUser() {
  localStorage.removeItem('xgy_web_user')
}

export function canView(module) {
  const user = getUser()
  if (!user) return false
  const perm = PERMISSIONS[user.role]?.[module]
  return perm && perm !== 'none'
}

export function canEdit(module) {
  const user = getUser()
  if (!user) return false
  return PERMISSIONS[user.role]?.[module] === 'edit'
}

export function financeMode() {
  const user = getUser()
  return PERMISSIONS[user?.role]?.finance || 'none'
}

export const DEMO_ACCOUNTS = [
  {
    phone: '13800000003',
    name: '赵教务',
    role: WEB_ROLES.ACADEMIC,
    campus: '城南校区',
    desc: '学员/老师/课程/教案可维护，聚焦排课运营'
  },
  {
    phone: '13800000004',
    name: '陈合伙人',
    role: WEB_ROLES.PARTNER,
    campus: '城南校区',
    shareRatio: 0.15,
    desc: '查看本校区业绩与分成财务，业务数据只读'
  },
  {
    phone: '13800000000',
    name: '周总（管理员）',
    role: WEB_ROLES.ADMIN,
    campus: '总部',
    shareRatio: 0.3,
    desc: '全校区财务、账号权限与全部配置'
  }
]

export const NAV_ITEMS = [
  { to: '/', label: '工作台', end: true, module: null },
  { to: '/students', label: '学员管理', module: 'students' },
  { to: '/teachers', label: '教师管理', module: 'teachers' },
  { to: '/courses', label: '课程管理', module: 'courses' },
  { to: '/packages', label: '教案管理', module: 'packages' },
  { to: '/finance', label: '财务中心', module: 'finance' },
  { to: '/users', label: '账号权限', module: 'users' }
]
