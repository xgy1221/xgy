/**
 * 远程数据桥：登录后水合本地 storage，写操作优先走 API。
 * 后端不可用时回退 mock.loginByPhone + 本地种子。
 */
const api = require('./api')
const auth = require('../utils/auth')
const { loginByPhone } = require('./mock')
const { todayKey, addDays } = require('../utils/date')

const ORG_KEY = 'xgy_orgs'
const STU_KEY = 'xgy_students'
const PKG_KEY = 'xgy_packages'
const EN_KEY = 'xgy_enrollments'
const LESSON_KEY = 'xgy_lessons'
const ACT_KEY = 'xgy_activities'
const SIGN_KEY = 'xgy_activity_signups'
const REMOTE_FLAG = 'xgy_remote_cache'

function orgCodeOf(item, fallback) {
  return (item && (item.orgCode || item.orgId)) || fallback || ''
}

function upsertOrgs(remoteOrgs) {
  if (!remoteOrgs || !remoteOrgs.length) return
  const local = wx.getStorageSync(ORG_KEY) || []
  const byId = {}
  local.forEach((o) => {
    byId[o.id] = o
  })
  remoteOrgs.forEach((o) => {
    const code = o.code || o.id
    const shortName =
      (o.shortName || o.name || '')
        .replace(/思维|英语|教育|培训机构|培训/g, '')
        .slice(0, 2) || (o.name || '').slice(0, 2)
    byId[code] = {
      id: code,
      numericId: o.id,
      name: o.name,
      shortName,
      campuses: o.campuses || (byId[code] && byId[code].campuses) || []
    }
  })
  wx.setStorageSync(ORG_KEY, Object.keys(byId).map((k) => byId[k]))
}

function mergeById(key, rows) {
  const existing = wx.getStorageSync(key) || []
  const map = {}
  existing.forEach((r) => {
    if (r && r.id != null) map[String(r.id)] = r
  })
  ;(rows || []).forEach((r) => {
    if (r && r.id != null) map[String(r.id)] = r
  })
  wx.setStorageSync(key, Object.keys(map).map((k) => map[k]))
}

function replaceKey(key, rows) {
  wx.setStorageSync(key, rows || [])
}

async function hydrateAfterLogin(session) {
  if (!session || !session.remote || !session.token) return session

  try {
    const orgs = await api.fetchOrgs().catch(() => [])
    upsertOrgs(orgs)

    const orgCodeByNumeric = {}
    ;(wx.getStorageSync(ORG_KEY) || []).forEach((o) => {
      if (o.numericId != null) orgCodeByNumeric[String(o.numericId)] = o.id
    })
    const resolveOrg = (numericOrCode) => {
      if (numericOrCode == null) return session.currentOrgId || ''
      const s = String(numericOrCode)
      if (orgCodeByNumeric[s]) return orgCodeByNumeric[s]
      return s
    }

    let students = session.remoteStudents || []
    if (!students.length) {
      const raw = await api.fetchStudents().catch(() => [])
      students = (raw || []).map(api.normalizeStudent)
    } else {
      students = students.map((s) => ({
        ...s,
        orgId: s.orgCode || resolveOrg(s.orgNumericId || s.orgId),
        parentPhone: s.parentPhone || session.user.phone
      }))
    }
    students = students.map((s) => ({
      ...s,
      orgId: s.orgCode || resolveOrg(s.orgNumericId || s.orgId),
      parentPhone: s.parentPhone || session.user.phone,
      parentName: s.parentName || session.user.name
    }))
    // 远程会话用后端学员覆盖本地，避免与种子 id 混用
    replaceKey(STU_KEY, students)

    // 教案：员工按机构；家长按当前机构（切换孩子后可再拉）
    const packagesRaw = await api.fetchPackages().catch(() => [])
    const packages = (packagesRaw || []).map((p) =>
      api.normalizePackage(p, resolveOrg(p.orgId))
    )
    if (packages.length) {
      const orgCodes = {}
      packages.forEach((p) => {
        orgCodes[p.orgId] = true
      })
      const prev = (wx.getStorageSync(PKG_KEY) || []).filter((p) => !orgCodes[p.orgId])
      replaceKey(PKG_KEY, prev.concat(packages))
    }

    const enrollments = []
    const studentIds =
      students.length > 0
        ? students.map((s) => s.id)
        : session.currentStudentId
          ? [session.currentStudentId]
          : []
    for (let i = 0; i < studentIds.length; i++) {
      const sid = studentIds[i]
      const list = await api.fetchEnrollments(sid).catch(() => [])
      ;(list || []).forEach((e) => {
        enrollments.push(api.normalizeEnrollment(e, resolveOrg(e.orgId)))
      })
    }
    // 覆盖这些学员的报读
    const sidSet = {}
    studentIds.forEach((id) => {
      sidSet[String(id)] = true
    })
    const keepEn = (wx.getStorageSync(EN_KEY) || []).filter((e) => !sidSet[String(e.studentId)])
    replaceKey(EN_KEY, keepEn.concat(enrollments))

    const from = addDays(todayKey(), -40)
    const to = addDays(todayKey(), 60)
    const lessonsRaw = await api.fetchLessonsByRange(from, to).catch(() => [])
    const lessons = (lessonsRaw || []).map((l) => {
      const n = api.normalizeLesson(l, resolveOrg(l.orgId))
      // 列表接口默认不带名单；家长端已按当前学员过滤，补一条占位以便本地日历筛选
      if ((!n.attendees || !n.attendees.length) && session.currentStudentId) {
        n.attendees = [
          {
            studentId: String(session.currentStudentId),
            studentName: '',
            type: 'regular',
            homeClassId: '',
            enrollmentId: '',
            teacherRated: false,
            studentRated: false,
            teacherRating: null,
            studentRating: null,
            consumed: false,
            absent: false
          }
        ]
      }
      return n
    })
    // 补拉少量详情（含评价字段）
    if (session.currentStudentId && lessons.length) {
      const detailLimit = Math.min(lessons.length, 8)
      for (let i = 0; i < detailLimit; i++) {
        try {
          const detail = await api.fetchLessonDetail(lessons[i].id)
          lessons[i] = api.normalizeLesson(detail, resolveOrg(detail.orgId))
        } catch (e) {
          // keep list row
        }
      }
    }
    if (lessons.length) {
      const orgCode = session.currentOrgId || resolveOrg(session.numericOrgId)
      const prev = (wx.getStorageSync(LESSON_KEY) || []).filter((l) => l.orgId !== orgCode)
      replaceKey(LESSON_KEY, prev.concat(lessons))
    }

    const sid = session.currentStudentId
    if (sid || session.currentOrgId) {
      const [open, past, mine] = await Promise.all([
        api.fetchActivities('open', sid).catch(() => []),
        api.fetchActivities('past', sid).catch(() => []),
        api.fetchActivities('mine', sid).catch(() => [])
      ])
      const orgCode = session.currentOrgId || resolveOrg(session.numericOrgId)
      const acts = []
      const seen = {}
      ;[...(open || []), ...(past || [])].forEach((a) => {
        const n = api.normalizeActivity(a, orgCode, sid)
        if (!seen[n.id]) {
          seen[n.id] = true
          acts.push(n)
        }
      })
      const prevActs = (wx.getStorageSync(ACT_KEY) || []).filter((a) => a.orgId !== orgCode)
      replaceKey(ACT_KEY, prevActs.concat(acts))

      const signups = (mine || []).map((a) => {
        const n = api.normalizeActivity(a, orgCode, sid)
        return {
          id: n.signupId || `as_${n.id}_${n.studentId || sid}`,
          activityId: n.id,
          orgId: n.orgId,
          studentId: n.studentId || sid,
          studentName: n.studentName || '',
          parentPhone: session.user.phone,
          status: 'confirmed',
          createdAt: Date.now()
        }
      })
      const prevSigns = (wx.getStorageSync(SIGN_KEY) || []).filter((s) => s.orgId !== orgCode)
      replaceKey(SIGN_KEY, prevSigns.concat(signups))
    }

    wx.setStorageSync(REMOTE_FLAG, { at: Date.now(), phone: session.user.phone })
  } catch (e) {
    console.warn('[bridge] hydrate failed', e)
  }
  return session
}

async function login(phone) {
  if (!/^1\d{10}$/.test(phone || '')) {
    return { ok: false, message: '请输入正确的 11 位手机号' }
  }

  const useRemote = await api.shouldUseRemote()
  if (useRemote) {
    try {
      const data = await api.login(phone, '123456')
      let session = api.mapAuthToSession(data)
      if (session.currentRole) {
        auth.setLastRole(phone, session.currentRole)
      }
      if (session.currentOrgId) {
        auth.setLastOrg(phone, session.currentOrgId)
      }
      // 先写入 token，供后续 hydrate 带 Authorization
      auth.setSession(session)
      session = await hydrateAfterLogin(session)
      auth.setSession(session)
      return { ok: true, session }
    } catch (e) {
      console.warn('[bridge] remote login failed, fallback local', e)
      // auto 模式下回退本地
      if (api.config.useRemote === true) {
        return { ok: false, message: (e && e.message) || '登录失败' }
      }
    }
  }

  const local = loginByPhone(phone)
  if (local.ok && local.session) {
    local.session.remote = false
  }
  return local
}

async function applyAuthPayload(data) {
  let session = api.mapAuthToSession(data)
  // 保留原 token 若 me 无 token
  const prev = auth.getSession()
  if (!session.token && prev && prev.token) session.token = prev.token
  auth.setSession(session)
  session = await hydrateAfterLogin(session)
  auth.setSession(session)
  return session
}

async function remoteSwitchStudent(studentId) {
  if (!api.isRemoteSession()) {
    auth.setCurrentStudentId(studentId)
    return auth.getSession()
  }
  const data = await api.switchStudent(studentId)
  return applyAuthPayload(data)
}

async function remoteSwitchRole(role) {
  if (!api.isRemoteSession()) {
    auth.setCurrentRole(role)
    return auth.getSession()
  }
  const session = auth.getSession()
  const orgId = (session && session.numericOrgId) || (session.user && session.user.numericOrgId)
  const data = await api.switchRole(role, orgId)
  return applyAuthPayload(data)
}

async function remoteLogout() {
  if (api.isRemoteSession()) {
    try {
      await api.logout()
    } catch (e) {
      // ignore
    }
  }
  auth.clearSession()
}

async function refreshActivitiesForCurrentStudent() {
  if (!api.isRemoteSession()) return null
  const session = auth.getSession()
  const sid = session.currentStudentId
  if (!sid) return null
  const orgCode = session.currentOrgId
  const [open, past, mine] = await Promise.all([
    api.fetchActivities('open', sid).catch(() => []),
    api.fetchActivities('past', sid).catch(() => []),
    api.fetchActivities('mine', sid).catch(() => [])
  ])
  const acts = []
  const seen = {}
  ;[...(open || []), ...(past || [])].forEach((a) => {
    const n = api.normalizeActivity(a, orgCode, sid)
    if (!seen[n.id]) {
      seen[n.id] = true
      acts.push(n)
    }
  })
  if (acts.length) mergeById(ACT_KEY, acts)
  const signups = (mine || []).map((a) => {
    const n = api.normalizeActivity(a, orgCode, sid)
    return {
      id: n.signupId || `as_${n.id}_${n.studentId || sid}`,
      activityId: n.id,
      orgId: n.orgId,
      studentId: n.studentId || sid,
      studentName: n.studentName || '',
      parentPhone: session.user.phone,
      status: 'confirmed',
      createdAt: Date.now()
    }
  })
  replaceKey(SIGN_KEY, signups)
  return { open: acts.filter((a) => !a.isPast), past: acts.filter((a) => a.isPast), mine: signups }
}

async function signupActivityRemote(payload) {
  if (!api.isRemoteSession()) return null
  const data = await api.signupActivity(payload.activityId, payload.studentId)
  await refreshActivitiesForCurrentStudent()
  return { ok: true, signup: data }
}

async function cancelSignupRemote(signupId) {
  if (!api.isRemoteSession()) return null
  await api.cancelActivitySignup(signupId)
  await refreshActivitiesForCurrentStudent()
  return { ok: true }
}

async function fetchPackageLessonsRemote(studentId, packageId) {
  if (!api.isRemoteSession()) return null
  const rows = await api.fetchStudentPackageLessons(studentId, packageId)
  return (rows || []).map(api.normalizePackageLessonRow)
}

module.exports = {
  login,
  hydrateAfterLogin,
  remoteSwitchStudent,
  remoteSwitchRole,
  remoteLogout,
  refreshActivitiesForCurrentStudent,
  signupActivityRemote,
  cancelSignupRemote,
  fetchPackageLessonsRemote,
  orgCodeOf
}
