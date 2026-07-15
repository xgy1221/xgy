/** 机构（租户）目录：多教培共用同一套产品时的隔离边界 */

const STORAGE_ORGS = 'xgy_orgs'

const SEED_ORGS = [
  {
    id: 'org_xuequ',
    name: '学趣思维',
    shortName: '学趣',
    campuses: ['城南校区', '高新校区', '总部']
  },
  {
    id: 'org_qihang',
    name: '启航英语',
    shortName: '启航',
    campuses: ['河东校区']
  }
]

function ensureOrgs() {
  const existing = wx.getStorageSync(STORAGE_ORGS)
  if (existing && Array.isArray(existing) && existing.length) return existing
  wx.setStorageSync(STORAGE_ORGS, SEED_ORGS)
  return SEED_ORGS.slice()
}

function listOrgs() {
  return ensureOrgs().slice()
}

function getOrgById(id) {
  if (!id) return null
  return listOrgs().find((o) => o.id === id) || null
}

function getOrgName(id) {
  const org = getOrgById(id)
  return org ? org.name : ''
}

function getOrgShortName(id) {
  const org = getOrgById(id)
  return (org && (org.shortName || org.name)) || ''
}

function decorateWithOrg(item) {
  if (!item) return item
  return {
    ...item,
    orgName: getOrgName(item.orgId),
    orgShortName: getOrgShortName(item.orgId)
  }
}

module.exports = {
  SEED_ORGS,
  ensureOrgs,
  listOrgs,
  getOrgById,
  getOrgName,
  getOrgShortName,
  decorateWithOrg
}
