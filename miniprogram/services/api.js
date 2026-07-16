/**
 * 后端 API 适配层（逐步替换本地 mock）。
 * 默认关闭：useRemote=false，继续走本地 storage 演示。
 * 接真后端时：改 config.baseUrl，并将 useRemote 设为 true。
 */
const auth = require('../utils/auth')

const config = {
  useRemote: false,
  baseUrl: 'http://localhost:8080',
  /** 小程序 role → 后端 RoleType */
  roleToBackend: {
    student: 'PARENT',
    teacher: 'TEACHER',
    academic: 'ACADEMIC',
    partner: 'PARTNER',
    admin: 'ADMIN'
  },
  roleFromBackend: {
    PARENT: 'student',
    TEACHER: 'teacher',
    ACADEMIC: 'academic',
    PARTNER: 'partner',
    ADMIN: 'admin'
  }
}

function getToken() {
  const session = auth.getSession()
  return (session && session.token) || ''
}

function request({ path, method = 'GET', data }) {
  if (!config.useRemote) {
    return Promise.reject(new Error('remote API disabled'))
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.baseUrl}${path}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        Authorization: getToken() ? `Bearer ${getToken()}` : ''
      },
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 401) {
          auth.clearSession()
          wx.reLaunch({ url: '/pages/login/login' })
          reject(new Error('未登录'))
          return
        }
        if (body.code !== 0) {
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve(body.data)
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

function login(phone, smsCode = '123456') {
  return request({ path: '/api/auth/login', method: 'POST', data: { phone, smsCode } })
}

function fetchEnrollments(studentId) {
  return request({ path: `/api/enrollments?studentId=${studentId}` })
}

function fetchStudentPackageLessons(studentId, packageId) {
  return request({
    path: `/api/lessons/student-package?studentId=${studentId}&packageId=${packageId}`
  })
}

function fetchActivities(tab) {
  return request({ path: `/api/activities?tab=${tab || 'open'}` })
}

function signupActivity(activityId, studentId) {
  return request({
    path: `/api/activities/${activityId}/signup`,
    method: 'POST',
    data: { studentId }
  })
}

module.exports = {
  config,
  request,
  login,
  fetchEnrollments,
  fetchStudentPackageLessons,
  fetchActivities,
  signupActivity
}
