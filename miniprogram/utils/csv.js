/**
 * 轻量 CSV 解析（支持逗号分隔、双引号包裹）
 * Excel 可另存为 CSV 后直接导入；表头需与模板一致。
 */

function parseCsv(text) {
  const rows = []
  let row = []
  let cell = ''
  let inQuotes = false
  const src = String(text || '').replace(/^\uFEFF/, '')

  for (let i = 0; i < src.length; i += 1) {
    const ch = src[i]
    const next = src[i + 1]

    if (inQuotes) {
      if (ch === '"' && next === '"') {
        cell += '"'
        i += 1
      } else if (ch === '"') {
        inQuotes = false
      } else {
        cell += ch
      }
      continue
    }

    if (ch === '"') {
      inQuotes = true
    } else if (ch === ',') {
      row.push(cell.trim())
      cell = ''
    } else if (ch === '\n') {
      row.push(cell.trim())
      if (row.some((v) => v !== '')) rows.push(row)
      row = []
      cell = ''
    } else if (ch === '\r') {
      // ignore
    } else {
      cell += ch
    }
  }

  if (cell !== '' || row.length) {
    row.push(cell.trim())
    if (row.some((v) => v !== '')) rows.push(row)
  }

  return rows
}

function rowsToObjects(rows) {
  if (!rows.length) return []
  const headers = rows[0].map((h) => h.trim())
  return rows.slice(1).map((cols) => {
    const obj = {}
    headers.forEach((key, idx) => {
      obj[key] = cols[idx] != null ? String(cols[idx]).trim() : ''
    })
    return obj
  })
}

module.exports = {
  parseCsv,
  rowsToObjects
}
