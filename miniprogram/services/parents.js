const STORAGE_KEY = 'xgy_parents'
const WHITELIST_KEY = 'xgy_phone_whitelist'

/** 白名单按机构隔离：同一手机号可出现在多家机构名单中 */
const SEED_WHITELIST = [
  {
    phone: '13800000001',
    orgId: 'org_xuequ',
    parentName: '王女士',
    note: '学趣完整导入示例',
    createdAt: Date.now()
  },
  {
    phone: '13800000001',
    orgId: 'org_qihang',
    parentName: '王女士',
    note: '启航英语报读',
    createdAt: Date.now()
  },
  {
    phone: '13800000031',
    orgId: 'org_xuequ',
    parentName: '',
    note: '仅录手机号，待家长自助完善',
    createdAt: Date.now()
  },
  {
    phone: '13800000051',
    orgId: 'org_qihang',
    parentName: '林女士',
    note: '启航学员家长',
    createdAt: Date.now()
  }
]

function ensureWhitelist() {
  const existing = wx.getStorageSync(WHITELIST_KEY)
  if (existing && Array.isArray(existing) && existing.length) return existing
  wx.setStorageSync(WHITELIST_KEY, SEED_WHITELIST)
  return SEED_WHITELIST.slice()
}

function getWhitelist(orgId) {
  const list = ensureWhitelist().slice()
  if (!orgId) return list
  return list.filter((i) => i.orgId === orgId)
}

function addPhoneToWhitelist(phone, parentName, note, orgId) {
  if (!orgId) return { ok: false, message: '缺少机构' }
  const list = ensureWhitelist()
  if (list.some((i) => i.phone === phone && i.orgId === orgId)) {
    return { ok: false, message: '该手机号已在本机构名单中' }
  }
  const item = {
    phone,
    orgId,
    parentName: parentName || '',
    note: note || '仅录手机号',
    createdAt: Date.now()
  }
  list.unshift(item)
  wx.setStorageSync(WHITELIST_KEY, list)
  return { ok: true, item }
}

function isPhoneKnown(phone, orgId) {
  const list = ensureWhitelist()
  if (orgId) return list.some((i) => i.phone === phone && i.orgId === orgId)
  return list.some((i) => i.phone === phone)
}

function listOrgIdsForPhone(phone) {
  return ensureWhitelist()
    .filter((i) => i.phone === phone)
    .map((i) => i.orgId)
    .filter(Boolean)
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
 * - 全新号码：自动建档，进入引导（需选择机构）
 */
function ensureParentAccess(phone, hintName, orgId) {
  const whitelist = ensureWhitelist()
  let row = null
  if (orgId) {
    row = whitelist.find((i) => i.phone === phone && i.orgId === orgId)
  } else {
    row = whitelist.find((i) => i.phone === phone)
  }
  if (!row) {
    row = {
      phone,
      orgId: orgId || '',
      parentName: hintName || '',
      note: '家长自主注册',
      createdAt: Date.now()
    }
    if (orgId) {
      whitelist.unshift(row)
      wx.setStorageSync(WHITELIST_KEY, whitelist)
    }
  }
  return ensureParentProfile(phone, {
    parentName: hintName || row.parentName || ''
  })
}

module.exports = {
  getWhitelist,
  addPhoneToWhitelist,
  isPhoneKnown,
  listOrgIdsForPhone,
  ensureParentProfile,
  getParentProfile,
  markOnboarded,
  ensureParentAccess
}
