const STORAGE_KEY = 'xgy_students'

/** Excel / CSV 标准列（导入时按列名识别） */
const IMPORT_HEADERS = {
  studentName: ['学生姓名', '姓名', '学员姓名'],
  parentPhone: ['家长手机号', '手机号', '家长电话', '联系电话'],
  parentName: ['家长姓名', '家长', '监护人'],
  grade: ['年级', '就读年级'],
  campus: ['校区', '分校'],
  remark: ['备注', '说明']
}

const SEED_STUDENTS = [
  {
    id: 'stu_yn',
    studentName: '王一诺',
    parentPhone: '13800000001',
    parentName: '王女士',
    grade: '小学四年级',
    campus: '城南校区',
    remark: '数学思维班'
  },
  {
    id: 'stu_yr',
    studentName: '王一然',
    parentPhone: '13800000001',
    parentName: '王女士',
    grade: '小学一年级',
    campus: '城南校区',
    remark: '同家庭二孩'
  },
  {
    id: 'stu_lz',
    studentName: '刘梓轩',
    parentPhone: '13800000011',
    parentName: '刘先生',
    grade: '小学四年级',
    campus: '城南校区',
    remark: ''
  },
  {
    id: 'stu_cs',
    studentName: '陈思琪',
    parentPhone: '13800000012',
    parentName: '陈女士',
    grade: '小学五年级',
    campus: '高新校区',
    remark: ''
  }
]

/** 演示「Excel 内容」：同手机号两名学生 */
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

function getStudentsByPhone(phone) {
  if (!phone) return []
  return getAllStudents().filter((s) => s.parentPhone === phone)
}

function getStudentById(id) {
  return getAllStudents().find((s) => s.id === id) || null
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

function normalizeRow(row) {
  return {
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
  if (!item.studentName) errors.push('缺少学生姓名')
  if (!item.parentPhone) errors.push('缺少家长手机号')
  else if (!/^1\d{10}$/.test(item.parentPhone)) errors.push('家长手机号格式不正确')
  return errors
}

function makeId(item) {
  return `stu_${item.parentPhone}_${item.studentName}`
}

/**
 * 导入学员行：同一家长手机号可对应多名学生
 * @returns {{ created, updated, skipped, families, errors, list }}
 */
function importStudentRows(rawRows) {
  const list = getAllStudents()
  const index = new Map(list.map((s) => [`${s.parentPhone}__${s.studentName}`, s]))
  let created = 0
  let updated = 0
  let skipped = 0
  const errors = []

  ;(rawRows || []).forEach((raw, idx) => {
    const item = normalizeRow(raw)
    const rowErrors = validateStudent(item)
    if (rowErrors.length) {
      skipped += 1
      errors.push({ row: idx + 2, message: rowErrors.join('；'), raw: item })
      return
    }

    const key = `${item.parentPhone}__${item.studentName}`
    const existed = index.get(key)
    if (existed) {
      Object.assign(existed, item, { id: existed.id })
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
    families: groupByPhone(list),
    list
  }
}

function groupByPhone(list) {
  const map = new Map()
  ;(list || getAllStudents()).forEach((s) => {
    if (!map.has(s.parentPhone)) {
      map.set(s.parentPhone, {
        phone: s.parentPhone,
        parentName: s.parentName || '家长',
        campus: s.campus || '',
        students: []
      })
    }
    const family = map.get(s.parentPhone)
    family.students.push(s)
    if (!family.parentName && s.parentName) family.parentName = s.parentName
  })

  return Array.from(map.values()).sort((a, b) => b.students.length - a.students.length)
}

function importDemoExcel() {
  return importStudentRows(DEMO_EXCEL_ROWS)
}

function getTemplateHint() {
  return {
    fileName: '学员导入模板.csv',
    columns: ['学生姓名', '家长手机号', '家长姓名', '年级', '校区', '备注'],
    note: '一个家长手机号可出现多行，分别对应多个孩子；家长登录后可切换学员。'
  }
}

function resetToSeed() {
  saveAllStudents(SEED_STUDENTS.slice())
  return getAllStudents()
}

module.exports = {
  IMPORT_HEADERS,
  DEMO_EXCEL_ROWS,
  getAllStudents,
  getStudentsByPhone,
  getStudentById,
  importStudentRows,
  importDemoExcel,
  groupByPhone,
  getTemplateHint,
  resetToSeed,
  normalizeRow
}
