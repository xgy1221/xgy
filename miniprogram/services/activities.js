const { todayKey, addDays, formatDisplay } = require('../utils/date')
const orgs = require('./orgs')

const ACT_KEY = 'xgy_activities'
const SIGN_KEY = 'xgy_activity_signups'

/**
 * 机构日常组织的比赛 / 赛事 / 展示活动
 * 家长端第二栏：报名参赛 + 往期赛果回顾
 */
function buildSeed() {
  const today = todayKey()
  return [
    {
      id: 'act_xuequ_math_cup',
      orgId: 'org_xuequ',
      title: '校内数学思维挑战赛',
      category: '学科竞赛',
      coverTone: 'teal',
      campus: '城南校区',
      address: '城南校区 B 栋考场',
      startDate: addDays(today, 8),
      startTime: '13:30',
      endTime: '16:30',
      enrollDeadline: addDays(today, 5),
      capacity: 60,
      fee: 0,
      targetGrade: '小学4-6年级',
      summary:
        '笔试 + 限时擂台，检验阶段学习成果。优胜者进入市级杯赛集训名单，颁发奖牌与证书。',
      highlights: ['校内选拔', '奖牌证书', '杯赛集训名额'],
      gallery: [],
      recap: '',
      published: true
    },
    {
      id: 'act_xuequ_abacus',
      orgId: 'org_xuequ',
      title: '速算王争霸赛',
      category: '趣味赛事',
      coverTone: 'amber',
      campus: '城南校区',
      address: '城南校区多功能厅',
      startDate: addDays(today, 15),
      startTime: '10:00',
      endTime: '12:00',
      enrollDeadline: addDays(today, 12),
      capacity: 40,
      fee: 0,
      targetGrade: '小学2-4年级',
      summary: '分组闯关比拼速算与观察力，适合低年级学员练胆量和专注力。家长可现场观赛。',
      highlights: ['分组闯关', '家长观赛', '人气奖'],
      gallery: [],
      recap: '',
      published: true
    },
    {
      id: 'act_xuequ_code_hack',
      orgId: 'org_xuequ',
      title: '少儿编程作品赛',
      category: '作品赛',
      coverTone: 'deep',
      campus: '高新校区',
      address: '高新校区创客教室',
      startDate: addDays(today, 22),
      startTime: '14:00',
      endTime: '17:00',
      enrollDeadline: addDays(today, 18),
      capacity: 28,
      fee: 30,
      targetGrade: '小学3-6年级',
      summary: '提交 Scratch/创意作品并现场演示，评委打分。报名费含评审与作品展板打印。',
      highlights: ['现场演示', '专业点评', '优秀作品展'],
      gallery: [],
      recap: '',
      published: true
    },
    {
      id: 'act_xuequ_past_festival',
      orgId: 'org_xuequ',
      title: '新年数学嘉年华挑战',
      category: '主题赛事',
      coverTone: 'teal',
      campus: '城南校区',
      address: '城南校区操场与教室',
      startDate: addDays(today, -40),
      startTime: '09:30',
      endTime: '12:00',
      enrollDeadline: addDays(today, -45),
      capacity: 100,
      fee: 0,
      targetGrade: '全年级',
      summary: '节日主题闯关赛：亲子协作完成 6 关数学挑战。',
      highlights: ['亲子闯关', '积分排行', '拍照打卡'],
      gallery: ['开赛合影', '速算擂台', '颁奖瞬间'],
      recap: '当天 86 组家庭完赛，孩子们完成 6 关挑战；金牌组获寒假体验课名额。',
      published: true
    },
    {
      id: 'act_xuequ_past_match',
      orgId: 'org_xuequ',
      title: '春季思维挑战赛（已结束）',
      category: '学科竞赛',
      coverTone: 'amber',
      campus: '城南校区',
      address: '城南校区 B 栋',
      startDate: addDays(today, -18),
      startTime: '13:30',
      endTime: '16:30',
      enrollDeadline: addDays(today, -25),
      capacity: 60,
      fee: 0,
      targetGrade: '小学4-6年级',
      summary: '上学期校内选拔，优胜者已进入杯赛集训。',
      highlights: ['笔试+擂台', '金银铜奖', '集训名额'],
      gallery: ['开赛动员', '认真作答', '获奖合影'],
      recap: '52 名学员参赛，评出金银铜奖；12 人进入市级杯赛集训营。',
      published: true
    },
    {
      id: 'act_qihang_speech',
      orgId: 'org_qihang',
      title: '少儿英语演讲比赛',
      category: '口语赛事',
      coverTone: 'teal',
      campus: '河东校区',
      address: '河东校区小剧场',
      startDate: addDays(today, 9),
      startTime: '15:00',
      endTime: '17:30',
      enrollDeadline: addDays(today, 6),
      capacity: 36,
      fee: 0,
      targetGrade: '小学全年级',
      summary: '自选主题 1–2 分钟英文演讲，现场打分。设最佳台风奖与进步奖。',
      highlights: ['现场演讲', '评委点评', '奖项证书'],
      gallery: [],
      recap: '',
      published: true
    },
    {
      id: 'act_qihang_past_show',
      orgId: 'org_qihang',
      title: '春季口语展示赛',
      category: '口语赛事',
      coverTone: 'deep',
      campus: '河东校区',
      address: '河东校区小剧场',
      startDate: addDays(today, -22),
      startTime: '15:00',
      endTime: '17:00',
      enrollDeadline: addDays(today, -28),
      capacity: 50,
      fee: 0,
      targetGrade: '全年级',
      summary: '短剧与演讲双赛道，记录孩子开口表达的成长。',
      highlights: ['短剧赛道', '演讲赛道', '家长观礼'],
      gallery: ['开场合唱', '短剧《超市购物》', '颁奖合影'],
      recap: '18 组节目参赛，现场座无虚席；回放已发班级群，可作往期参考。',
      published: true
    }
  ]
}

function ensureActivities() {
  const existing = wx.getStorageSync(ACT_KEY)
  if (existing && Array.isArray(existing) && existing.length) return existing
  const seed = buildSeed()
  wx.setStorageSync(ACT_KEY, seed)
  return seed.slice()
}

function ensureSignups() {
  const existing = wx.getStorageSync(SIGN_KEY)
  if (existing && Array.isArray(existing)) return existing
  wx.setStorageSync(SIGN_KEY, [])
  return []
}

function saveSignups(list) {
  wx.setStorageSync(SIGN_KEY, list)
}

function compareDate(a, b) {
  if (a === b) return 0
  return a < b ? -1 : 1
}

function getSignupCount(activityId) {
  return ensureSignups().filter((s) => s.activityId === activityId && s.status !== 'cancelled')
    .length
}

function decorate(activity, studentId) {
  const today = todayKey()
  const localEnrolled = getSignupCount(activity.id)
  // 远程水合会带上服务端报名人数；与本地取较大值，避免名额显示偏少
  const enrolled = Math.max(localEnrolled, Number(activity.enrolled) || 0)
  const isPast = compareDate(activity.startDate, today) < 0
  const deadlinePassed = compareDate(activity.enrollDeadline, today) < 0
  let enrollStatus = 'open'
  if (isPast) enrollStatus = 'past'
  else if (deadlinePassed) enrollStatus = 'closed'
  else if (activity.capacity > 0 && enrolled >= activity.capacity) enrollStatus = 'full'

  const mySignup = studentId
    ? ensureSignups().find(
        (s) =>
          String(s.activityId) === String(activity.id) &&
          String(s.studentId) === String(studentId) &&
          s.status !== 'cancelled'
      )
    : null

  const statusTextMap = {
    open: '报名中',
    full: '已满员',
    closed: '已截止',
    past: '已完赛'
  }

  const remain = Math.max(0, activity.capacity - enrolled)
  const feeText = activity.fee > 0 ? `¥${activity.fee}` : '免费'

  return {
    ...activity,
    orgName: orgs.getOrgName(activity.orgId),
    orgShortName: orgs.getOrgShortName(activity.orgId),
    enrolled,
    remain,
    enrollStatus,
    statusText: statusTextMap[enrollStatus],
    feeText,
    dateText: `${formatDisplay(activity.startDate)} ${activity.startTime}-${activity.endTime}`,
    deadlineText: formatDisplay(activity.enrollDeadline),
    signed: !!mySignup,
    signupId: mySignup ? mySignup.id : '',
    isPast,
    coverLabel: activity.category
  }
}

function listByOrg(orgId, studentId) {
  return ensureActivities()
    .filter((a) => a.orgId === orgId && a.published)
    .map((a) => decorate(a, studentId))
    .sort((a, b) => {
      if (a.isPast !== b.isPast) return a.isPast ? 1 : -1
      return compareDate(a.startDate, b.startDate)
    })
}

function listOpen(orgId, studentId) {
  return listByOrg(orgId, studentId).filter((a) => !a.isPast)
}

function listPast(orgId, studentId) {
  return listByOrg(orgId, studentId)
    .filter((a) => a.isPast)
    .sort((a, b) => compareDate(b.startDate, a.startDate))
}

function getById(id, studentId) {
  const raw = ensureActivities().find((a) => a.id === id)
  return raw ? decorate(raw, studentId) : null
}

function listMySignups(parentPhone, studentId) {
  const signs = ensureSignups().filter((s) => {
    if (s.status === 'cancelled') return false
    if (studentId) return s.studentId === studentId
    return s.parentPhone === parentPhone
  })
  return signs
    .map((s) => {
      const act = getById(s.activityId, s.studentId)
      if (!act) return null
      return {
        ...act,
        signupId: s.id,
        signupAt: s.createdAt,
        signupStatus: s.status,
        studentName: s.studentName
      }
    })
    .filter(Boolean)
    .sort((a, b) => (a.signupAt < b.signupAt ? 1 : -1))
}

function signup({ activityId, studentId, studentName, parentPhone, orgId }) {
  const act = getById(activityId, studentId)
  if (!act) return { ok: false, message: '活动不存在' }
  if (act.orgId !== orgId) return { ok: false, message: '不能跨机构报名' }
  if (act.enrollStatus === 'past') return { ok: false, message: '比赛已结束' }
  if (act.enrollStatus === 'closed') return { ok: false, message: '报名已截止' }
  if (act.enrollStatus === 'full') return { ok: false, message: '名额已满' }
  if (act.signed) return { ok: false, message: '已报名，无需重复提交' }

  const list = ensureSignups()
  const item = {
    id: `as_${activityId}_${studentId}_${Date.now()}`,
    activityId,
    orgId,
    studentId,
    studentName,
    parentPhone,
    status: 'confirmed',
    createdAt: Date.now()
  }
  list.unshift(item)
  saveSignups(list)
  return { ok: true, signup: item, activity: getById(activityId, studentId) }
}

function cancelSignup(signupId, parentPhone) {
  const list = ensureSignups()
  const item = list.find((s) => s.id === signupId)
  if (!item) return { ok: false, message: '报名记录不存在' }
  if (item.parentPhone !== parentPhone) return { ok: false, message: '无权取消' }
  const act = getById(item.activityId, item.studentId)
  if (act && act.isPast) return { ok: false, message: '比赛已开始/结束，无法取消' }
  item.status = 'cancelled'
  item.cancelledAt = Date.now()
  saveSignups(list)
  return { ok: true }
}

/** 开发调试：清空并重灌比赛演示数据 */
function resetSeed() {
  wx.removeStorageSync(ACT_KEY)
  wx.removeStorageSync(SIGN_KEY)
  return ensureActivities()
}

module.exports = {
  ensureActivities,
  listByOrg,
  listOpen,
  listPast,
  listMySignups,
  getById,
  signup,
  cancelSignup,
  resetSeed
}
