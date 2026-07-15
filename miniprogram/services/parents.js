const STORAGE_KEY = 'xgy_parents'
const WHITELIST_KEY = 'xgy_phone_whitelist'

/** 仅手机号白名单：机构可先录号，学员信息留给家长补全 */
const SEED_WHITELIST = [
  { phone: '13800000001', parentName: '王女士', note: '完整导入示例', createdAt: Date.now() },
  { phone: '13800000031', parentName: '', note: '仅录手机号，待家长自助完善', createdAt: Date.now() }
]

function ensureWhitelist() {
  const existing = wx.getStorageSync(WHITELIST_KEY)
  if (existing && Array.isArray(existing) && existing.length) return existing
  wx.setStorageSync(WHITELIST_KEY, SEED_WHITELIST)
  return SEED_WHITELIST.slice()
}

function getWhitelist() {
  return ensureWhitelist().slice()
}

function addPhoneToWhitelist(phone, parentName, note) {
  const list = getWhitelist()
  if (list.some((i) => i.phone === phone)) {
    return { ok: false, message: '该手机号已在名单中' }
  }
  const item = {
    phone,
    parentName: parentName || '',
    note: note || '仅录手机号',
    createdAt: Date.now()
  }
  list.unshift(item)
  wx.setStorageSync(WHITELIST_KEY, list)
  return { ok: true, item }
}

function isPhoneKnown(phone) {
  return getWhitelist().some((i) => i.phone === phone)
}

function ensureParentProfile(phone, patch) {
  const map = wx.getStorageSync(STORAGE_KEY) || {}
  const prev = map[phone] || {
    phone,
    parentName: '',
    onboarded: false,
    createdAt: Date.now()
  }
  const next = { ...prev, ...(patch || {}), phone, updatedAt: Date.now() }
  map[phone] = next
  wx.setStorageSync(STORAGE_KEY, map)
  return next
}

function getParentProfile(phone) {
  const map = wx.getStorageSync(STORAGE_KEY) || {}
  return map[phone] || null
}

function markOnboarded(phone) {
  return ensureParentProfile(phone, { onboarded: true })
}

/**
 * 任意有效手机号都可进入家长端：
 * - 白名单 / 已有学员：欢迎回来
 * - 全新号码：自动建档，进入引导
 */
function ensureParentAccess(phone, hintName) {
  const whitelist = getWhitelist()
  let row = whitelist.find((i) => i.phone === phone)
  if (!row) {
    row = {
      phone,
      parentName: hintName || '',
      note: '家长自主注册',
      createdAt: Date.now()
    }
    whitelist.unshift(row)
    wx.setStorageSync(WHITELIST_KEY, whitelist)
  }
  return ensureParentProfile(phone, {
    parentName: hintName || row.parentName || ''
  })
}

module.exports = {
  getWhitelist,
  addPhoneToWhitelist,
  isPhoneKnown,
  ensureParentProfile,
  getParentProfile,
  markOnboarded,
  ensureParentAccess
}
