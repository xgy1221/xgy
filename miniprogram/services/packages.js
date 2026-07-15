const STORAGE_KEY = 'xgy_packages'

const SEED_PACKAGES = [
  {
    id: 'pkg_xuequ_math_48',
    orgId: 'org_xuequ',
    name: '小学数学思维提升',
    grade: '小学3-5年级',
    lessonCount: 48,
    price: 3680,
    subject: '数学',
    status: '上架',
    outline: ['认识规律', '速算技巧', '应用题建模', '阶段测评']
  },
  {
    id: 'pkg_xuequ_en_24',
    orgId: 'org_xuequ',
    name: '英语阅读加油站',
    grade: '小学2-4年级',
    lessonCount: 24,
    price: 3280,
    subject: '英语',
    status: '上架',
    outline: ['绘本跟读', '词汇闯关', '短文理解', '口语表达']
  },
  {
    id: 'pkg_xuequ_code_32',
    orgId: 'org_xuequ',
    name: '编程启蒙 L1',
    grade: '小学3-6年级',
    lessonCount: 32,
    price: 4200,
    subject: '编程',
    status: '上架',
    outline: ['Scratch 入门', '动画故事', '简单游戏', '作品发布']
  },
  {
    id: 'pkg_xuequ_write_16',
    orgId: 'org_xuequ',
    name: '硬笔书写课',
    grade: '小学1-3年级',
    lessonCount: 16,
    price: 1680,
    subject: '书法',
    status: '上架',
    outline: ['握笔姿势', '基本笔画', '独体字', '篇章书写']
  },
  {
    id: 'pkg_qihang_en_36',
    orgId: 'org_qihang',
    name: '少儿英语进阶营',
    grade: '小学3-5年级',
    lessonCount: 36,
    price: 4580,
    subject: '英语',
    status: '上架',
    outline: ['自然拼读', '情景口语', '分级阅读', '阶段测评']
  },
  {
    id: 'pkg_qihang_speak_20',
    orgId: 'org_qihang',
    name: '口语角 20 课',
    grade: '小学全年级',
    lessonCount: 20,
    price: 1980,
    subject: '英语',
    status: '上架',
    outline: ['日常对话', '角色扮演', '自由表达']
  }
]

function ensureSeed() {
  const existing = wx.getStorageSync(STORAGE_KEY)
  if (existing && Array.isArray(existing) && existing.length) return existing
  wx.setStorageSync(STORAGE_KEY, SEED_PACKAGES)
  return SEED_PACKAGES.slice()
}

function getAllPackages() {
  return ensureSeed().slice()
}

function saveAllPackages(list) {
  wx.setStorageSync(STORAGE_KEY, list)
}

function listPackagesByOrg(orgId) {
  if (!orgId) return []
  return getAllPackages().filter((p) => p.orgId === orgId)
}

function getPackageById(id) {
  return getAllPackages().find((p) => p.id === id) || null
}

function getOnSalePackages(orgId) {
  const list = orgId ? listPackagesByOrg(orgId) : getAllPackages()
  return list.filter((p) => p.status === '上架')
}

function upsertPackage(pkg) {
  if (!pkg.orgId) return null
  const list = getAllPackages()
  const idx = list.findIndex((p) => p.id === pkg.id)
  if (idx >= 0) list[idx] = { ...list[idx], ...pkg }
  else list.push(pkg)
  saveAllPackages(list)
  return pkg
}

function togglePackageStatus(id) {
  const list = getAllPackages()
  const item = list.find((p) => p.id === id)
  if (!item) return null
  item.status = item.status === '上架' ? '下架' : '上架'
  saveAllPackages(list)
  return item
}

module.exports = {
  getAllPackages,
  listPackagesByOrg,
  getPackageById,
  getOnSalePackages,
  upsertPackage,
  togglePackageStatus
}
