/**
 * 本地演示数据 schema 版本。
 * 升版本会清空业务缓存并重种子，避免无 orgId 的旧数据串机构。
 */
const SCHEMA_KEY = 'xgy_data_schema'
const SCHEMA_VERSION = 6

const TENANT_KEYS = [
  'xgy_orgs',
  'xgy_students',
  'xgy_packages',
  'xgy_enrollments',
  'xgy_classes',
  'xgy_lessons',
  'xgy_parents',
  'xgy_phone_whitelist',
  'xgy_activities',
  'xgy_activity_signups'
]

function ensureSchema() {
  const current = wx.getStorageSync(SCHEMA_KEY)
  if (current === SCHEMA_VERSION) return false
  TENANT_KEYS.forEach((k) => {
    try {
      wx.removeStorageSync(k)
    } catch (e) {
      // ignore
    }
  })
  wx.setStorageSync(SCHEMA_KEY, SCHEMA_VERSION)
  return true
}

module.exports = {
  SCHEMA_VERSION,
  ensureSchema
}
