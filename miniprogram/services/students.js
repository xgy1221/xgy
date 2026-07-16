const orgs = require('./orgs')

const STORAGE_KEY = 'xgy_students'
const DEFAULT_ORG = 'org_xuequ'

/** Excel / CSV 标准列（导入时按列名识别） */
const IMPORT_HEADERS = {
  studentName: ['学生姓名', '姓名', '学员姓名'],
  parentPhone: ['家长手机号', '手机号', '家长电话', '联系电话'],
  parentName: ['家长姓名', '家长', '监护人'],
  grade: ['年级', '就读年级'],
  campus: ['校区', '分校'],
  remark: ['备注', '说明'],
  orgName: ['机构', '机构名称', '培训机构']
}

const SEED_STUDENTS = [
  {
    id: 'stu_xuequ_yn',
    orgId: 'org_xuequ',
    studentName: '王一诺',
    parentPhone: '13800000001',
    parentName: '王女士',
    grade: '小学四年级',
    campus: '城南校区',
    remark: '数学思维班'
  },
  {
    id: 'stu_xuequ_yr',
    orgId: 'org_xuequ',
    studentName: '王一然',
    parentPhone: '13800000001',
    parentName: '王女士',
    grade: '小学一年级',
    campus: '城南校区',
    remark: '同家庭二孩'
  },
  {
    id: 'stu_xuequ_lz',
    orgId: 'org_xuequ',
    studentName: '刘梓轩',
    parentPhone: '13800000011',
    parentName: '刘先生',
    grade: '小学四年级',
    campus: '城南校区',
    remark: ''
  },
  {
    id: 'stu_xuequ_cs',
    orgId: 'org_xuequ',
    studentName: '陈思琪',
    parentPhone: '13800000012',
    parentName: '陈女士',
    grade: '小学五年级',
    campus: '高新校区',
    remark: ''
  },
  /** 同家长在另一机构也有报读：隔离靠 orgId，不是靠改手机号 */
  {
    id: 'stu_qihang_yn',
    orgId: 'org_qihang',
    studentName: '王一诺',
    parentPhone: '13800000001',
    parentName: '王女士',
    grade: '小学四年级',
    campus: '河东校区',
    remark: '启航英语班'
  },
  {
    id: 'stu_qihang_ly',
    orgId: 'org_qihang',
    studentName: '林雨桐',
    parentPhone: '13800000051',
    parentName: '林女士',
    grade: '小学三年级',
    campus: '河东校区',
    remark: ''
  }
]

/** 演示「Excel 内容」：同手机号两名学生（默认导入到当前机构） */
const DEMO_EXCEL_ROWS = [
  {
    学生姓名: '张沐阳',
    家长手机号: '13800000021',
    家长姓名: '张女士',
    年级: '小学三年级',
    校区: '城南校区',
    备注: '编程兴趣'
  },
  {
    学生姓名: '张沐辰',
    家长手机号: '13800000021',
    家长姓名: '张女士',
    年级: '幼儿园大班',
    校区: '城南校区',
    备注: '同家老二'
  },
  {
    学生姓名: '苏晴',
    家长手机号: '13800000022',
    家长姓名: '苏先生',
    年级: '小学二年级',
    校区: '高新校区',
    备注: ''
  }
]

function ensureSeed() {
  const existing = wx.getStorageSync(STORAGE_KEY)
  if (existing && Array.isArray(existing) && existing.length) {
    return existing
  }
  wx.setStorageSync(STORAGE_KEY, SEED_STUDENTS)
  return SEED_STUDENTS.slice()
}

function getAllStudents() {
  return ensureSeed().slice()
}

function saveAllStudents(list) {
  wx.setStorageSync(STORAGE_KEY, list)
}

function decorate(s) {
  return orgs.decorateWithOrg(s)
}

/** 员工端：只看本机构学员 */
function listStudentsByOrg(orgId) {
  if (!orgId) return []
  return getAllStudents()
    .filter((s) => s.orgId === orgId)
    .map(decorate)
}

/** 家长端：跨机构列出所有孩子（切换孩子即切换机构上下文） */
function getStudentsByPhone(phone) {
  if (!phone) return []
  return getAllStudents()
    .filter((s) => s.parentPhone === phone)
    .map(decorate)
}

function getStudentById(id) {
  if (id == null || id === '') return null
  const s = getAllStudents().find((x) => String(x.id) === String(id))
  return s ? decorate(s) : null
}

function pickField(row, aliases) {
  const keys = Object.keys(row || {})
  for (let i = 0; i < aliases.length; i += 1) {
    const alias = aliases[i]
    if (row[alias] != null && String(row[alias]).trim() !== '') {
      return String(row[alias]).trim()
    }
    const matched = keys.find((k) => k.trim() === alias)
    if (matched && String(row[matched]).trim() !== '') {
      return String(row[matched]).trim()
    }
  }
  return ''
}

function resolveOrgIdFromRow(row, fallbackOrgId) {
  const name = pickField(row, IMPORT_HEADERS.orgName)
  if (name) {
    const found = orgs.listOrgs().find((o) => o.name === name || o.shortName === name)
    if (found) return found.id
  }
  return fallbackOrgId || DEFAULT_ORG
}

function normalizeRow(row, fallbackOrgId) {
  return {
    orgId: resolveOrgIdFromRow(row, fallbackOrgId),
    studentName: pickField(row, IMPORT_HEADERS.studentName),
    parentPhone: pickField(row, IMPORT_HEADERS.parentPhone).replace(/\s+/g, ''),
    parentName: pickField(row, IMPORT_HEADERS.parentName),
    grade: pickField(row, IMPORT_HEADERS.grade),
    campus: pickField(row, IMPORT_HEADERS.campus),
    remark: pickField(row, IMPORT_HEADERS.remark)
  }
}

function validateStudent(item) {
  const errors = []
  if (!item.orgId) errors.push('缺少所属机构')
  if (!item.studentName) errors.push('缺少学生姓名')
  if (!item.parentPhone) errors.push('缺少家长手机号')
  else if (!/^1\d{10}$/.test(item.parentPhone)) errors.push('家长手机号格式不正确')
  return errors
}

function makeId(item) {
  return `stu_${item.orgId}_${item.parentPhone}_${item.studentName}`
}

function studentKey(item) {
  return `${item.orgId}__${item.parentPhone}__${item.studentName}`
}

/**
 * 导入学员行：同一家长手机号可对应多名学生；唯一键含机构，避免跨机构串档
 */
function importStudentRows(rawRows, orgId) {
  const targetOrg = orgId || DEFAULT_ORG
  const list = getAllStudents()
  const index = new Map(list.map((s) => [studentKey(s), s]))
  let created = 0
  let updated = 0
  let skipped = 0
  const errors = []

  ;(rawRows || []).forEach((raw, idx) => {
    const item = normalizeRow(raw, targetOrg)
    // 未写机构列时强制进当前导入机构，防止串台
    item.orgId = targetOrg
    const rowErrors = validateStudent(item)
    if (rowErrors.length) {
      skipped += 1
      errors.push({ row: idx + 2, message: rowErrors.join('；'), raw: item })
      return
    }

    const key = studentKey(item)
    const existed = index.get(key)
    if (existed) {
      Object.assign(existed, item, { id: existed.id, orgId: existed.orgId })
      updated += 1
    } else {
      const next = { id: makeId(item), ...item }
      list.push(next)
      index.set(key, next)
      created += 1
    }
  })

  saveAllStudents(list)
  return {
    created,
    updated,
    skipped,
    errors,
    families: groupByPhone(listStudentsByOrg(targetOrg)),
    list: listStudentsByOrg(targetOrg)
  }
}

function groupByPhone(list) {
  const map = new Map()
  ;(list || []).forEach((s) => {
    const key = `${s.orgId}__${s.parentPhone}`
    if (!map.has(key)) {
      map.set(key, {
        phone: s.parentPhone,
        parentName: s.parentName || '家长',
        campus: s.campus || '',
        orgId: s.orgId,
        orgName: s.orgName || orgs.getOrgName(s.orgId),
        students: []
      })
    }
    const family = map.get(key)
    family.students.push(s)
    if (!family.parentName && s.parentName) family.parentName = s.parentName
  })

  return Array.from(map.values()).sort((a, b) => b.students.length - a.students.length)
}

function importDemoExcel(orgId) {
  return importStudentRows(DEMO_EXCEL_ROWS, orgId || DEFAULT_ORG)
}

function getTemplateHint() {
  return {
    fileName: '学员导入模板.csv',
    columns: ['学生姓名', '家长手机号', '家长姓名', '年级', '校区', '备注'],
    note: '导入写入当前登录员工所属机构；同手机号多名学生OK。跨机构请各自机构账号导入，不会互相覆盖。'
  }
}

function resetToSeed() {
  saveAllStudents(SEED_STUDENTS.slice())
  return getAllStudents()
}

/**
 * 家长自助 / 代录添加学员；唯一键 = 机构 + 手机号 + 姓名
 */
function upsertStudent(input) {
  const item = {
    orgId: input.orgId || DEFAULT_ORG,
    studentName: (input.studentName || '').trim(),
    parentPhone: (input.parentPhone || '').replace(/\s+/g, ''),
    parentName: (input.parentName || '').trim(),
    grade: (input.grade || '').trim(),
    campus: (input.campus || '').trim(),
    remark: (input.remark || '').trim()
  }
  const errors = validateStudent(item)
  if (errors.length) {
    return { ok: false, message: errors.join('；') }
  }

  const list = getAllStudents()
  const key = studentKey(item)
  const existed = list.find((s) => studentKey(s) === key)
  if (existed) {
    Object.assign(existed, item, { orgId: existed.orgId })
    saveAllStudents(list)
    return { ok: true, student: decorate(existed), created: false }
  }

  const student = { id: makeId(item), ...item }
  list.push(student)
  saveAllStudents(list)
  return { ok: true, student: decorate(student), created: true }
}

module.exports = {
  IMPORT_HEADERS,
  DEMO_EXCEL_ROWS,
  DEFAULT_ORG,
  getAllStudents,
  listStudentsByOrg,
  getStudentsByPhone,
  getStudentById,
  importStudentRows,
  importDemoExcel,
  groupByPhone,
  getTemplateHint,
  resetToSeed,
  normalizeRow,
  upsertStudent
}
