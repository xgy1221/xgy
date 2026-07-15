const { ROLE_META } = require('./constants')

const SESSION_KEY = 'xgy_session'
const LAST_ROLE_KEY = 'xgy_last_roles'

function getSession() {
  return wx.getStorageSync(SESSION_KEY) || null
}

function setSession(session) {
  wx.setStorageSync(SESSION_KEY, session)
  const app = getApp()
  if (app) {
    app.globalData.session = session
  }
}

function clearSession() {
  wx.removeStorageSync(SESSION_KEY)
  const app = getApp()
  if (app) {
    app.globalData.session = null
  }
}

function isLoggedIn() {
  const session = getSession()
  return !!(session && session.token)
}

function getLastRoleMap() {
  return wx.getStorageSync(LAST_ROLE_KEY) || {}
}

function getLastRole(phone) {
  if (!phone) return null
  return getLastRoleMap()[phone] || null
}

function setLastRole(phone, role) {
  if (!phone || !role) return
  const map = getLastRoleMap()
  map[phone] = role
  wx.setStorageSync(LAST_ROLE_KEY, map)
}

/**
 * 多角色时优先恢复「上次选择的角色」；仍有效才用，否则返回 null 让用户重选
 */
function resolvePreferredRole(user) {
  const roles = (user && user.roles) || []
  if (!roles.length) return null
  if (roles.length === 1) return roles[0]
  const last = getLastRole(user.phone)
  if (last && roles.indexOf(last) >= 0) return last
  return null
}

function getCurrentRole() {
  const session = getSession()
  return session ? session.currentRole : null
}

function setCurrentRole(role) {
  const session = getSession()
  if (!session) return
  session.currentRole = role
  setSession(session)
  if (session.user && session.user.phone) {
    setLastRole(session.user.phone, role)
  }
}

function getCurrentStudentId() {
  const session = getSession()
  return session ? session.currentStudentId : null
}

function setCurrentStudentId(studentId) {
  const session = getSession()
  if (!session) return
  session.currentStudentId = studentId
  setSession(session)
}

function getRoleHome(role) {
  return (ROLE_META[role] && ROLE_META[role].home) || '/pages/login/login'
}

function requireAuth() {
  if (!isLoggedIn()) {
    wx.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
}

function switchToRoleHome(role) {
  setCurrentRole(role)
  wx.reLaunch({ url: getRoleHome(role) })
}

module.exports = {
  getSession,
  setSession,
  clearSession,
  isLoggedIn,
  getLastRole,
  setLastRole,
  resolvePreferredRole,
  getCurrentRole,
  setCurrentRole,
  getCurrentStudentId,
  setCurrentStudentId,
  getRoleHome,
  requireAuth,
  switchToRoleHome
}
