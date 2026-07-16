/** Web 端角色：教务 / 合伙人 / 管理员 */

import { clearToken, getToken, request, setToken } from '../api/client'

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

const BACKEND_TO_WEB = {
  ACADEMIC: 'academic',
  PARTNER: 'partner',
  ADMIN: 'admin'
}

const WEB_TO_BACKEND = {
  academic: 'ACADEMIC',
  partner: 'PARTNER',
  admin: 'ADMIN'
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
  clearToken()
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

/** 演示账号：仅快捷填手机号，真实登录走后端短信码 123456 */
export const DEMO_ACCOUNTS = [
  {
    phone: '13800000003',
    name: '赵教务',
    role: WEB_ROLES.ACADEMIC,
    campus: '城南校区',
    orgId: 'org_xuequ',
    orgName: '学趣思维',
    desc: '学员/老师/课程/教案可维护，聚焦排课运营'
  },
  {
    phone: '13800000004',
    name: '陈合伙人',
    role: WEB_ROLES.PARTNER,
    campus: '城南校区',
    orgId: 'org_xuequ',
    orgName: '学趣思维',
    shareRatio: 0.15,
    desc: '查看本人渠道业绩与分成财务，业务数据只读'
  },
  {
    phone: '13800000000',
    name: '周总（管理员）',
    role: WEB_ROLES.ADMIN,
    campus: '总部',
    orgId: 'org_xuequ',
    orgName: '学趣思维',
    shareRatio: 0.3,
    desc: '全校区财务、账号权限与全部配置'
  },
  {
    phone: '13800000040',
    name: '启航教务',
    role: WEB_ROLES.ACADEMIC,
    campus: '河东校区',
    orgId: 'org_qihang',
    orgName: '启航英语',
    desc: '启航英语机构教务，仅可见本机构数据'
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

function pickWebBackendRole(roles) {
  const names = (roles || []).map((r) => (typeof r === 'string' ? r : r.role))
  for (const cand of ['ADMIN', 'PARTNER', 'ACADEMIC']) {
    if (names.includes(cand)) return cand
  }
  return null
}

function mapAuthToUser(data, preferredCampus) {
  const webRole = BACKEND_TO_WEB[data.currentRole]
  const roleRow = (data.roles || []).find((r) => r.role === data.currentRole)
  return {
    id: data.userId,
    phone: data.phone,
    name: data.name,
    role: webRole,
    campus: preferredCampus || '',
    orgId: data.orgCode || String(data.orgId || ''),
    orgNumericId: data.orgId,
    orgName: data.orgName || (roleRow && roleRow.orgName) || '',
    shareRatio: webRole === 'partner' ? 0.15 : webRole === 'admin' ? 0.3 : undefined,
    roles: (data.roles || []).map((r) => BACKEND_TO_WEB[r.role] || r.role).filter(Boolean)
  }
}

/** 手机号 + 短信登录，并切换到适合 Web 的角色 */
export async function loginWithSms(phone, smsCode = '123456', preferredCampus = '') {
  const data = await request('/api/auth/login', {
    method: 'POST',
    data: { phone, smsCode },
    auth: false
  })
  setToken(data.token)

  let auth = data
  const wanted = pickWebBackendRole(data.roles)
  if (!wanted) {
    clearUser()
    throw new Error('该账号无 Web 管理端角色（需要教务/合伙/管理员）')
  }
  if (data.currentRole !== wanted) {
    auth = await request('/api/auth/switch-role', {
      method: 'POST',
      data: { role: wanted, orgId: data.orgId }
    })
    if (auth.token) setToken(auth.token)
  }

  const user = mapAuthToUser(auth, preferredCampus)
  if (!user.role) {
    clearUser()
    throw new Error('无法解析管理端角色')
  }
  setUser(user)
  return user
}

export async function logoutRemote() {
  try {
    if (getToken()) {
      await request('/api/auth/logout', { method: 'POST', data: {} })
    }
  } catch {
    /* ignore */
  } finally {
    clearUser()
  }
}

export { WEB_TO_BACKEND, getToken }
