const { ROLE_META } = require('./constants')

const SESSION_KEY = 'xgy_session'

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

function getCurrentRole() {
  const session = getSession()
  return session ? session.currentRole : null
}

function setCurrentRole(role) {
  const session = getSession()
  if (!session) return
  session.currentRole = role
  setSession(session)
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

function requireAuth(pageThis) {
  if (!isLoggedIn()) {
    wx.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
}

function switchToRoleHome(role) {
  const url = getRoleHome(role)
  wx.reLaunch({ url })
}

module.exports = {
  getSession,
  setSession,
  clearSession,
  isLoggedIn,
  getCurrentRole,
  setCurrentRole,
  getCurrentStudentId,
  setCurrentStudentId,
  getRoleHome,
  requireAuth,
  switchToRoleHome
}
