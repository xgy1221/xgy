function pad(n) {
  return n < 10 ? `0${n}` : `${n}`
}

function toDateKey(date) {
  const d = date instanceof Date ? date : new Date(date)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function parseDateKey(key) {
  const parts = String(key || '').split('-').map(Number)
  return new Date(parts[0], parts[1] - 1, parts[2])
}

function todayKey() {
  return toDateKey(new Date())
}

function addDays(key, days) {
  const d = parseDateKey(key)
  d.setDate(d.getDate() + days)
  return toDateKey(d)
}

function formatDisplay(key) {
  const d = parseDateKey(key)
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 周${week}`
}

function getMonthMatrix(year, month) {
  // month: 1-12
  const first = new Date(year, month - 1, 1)
  const startWeekday = first.getDay() // 0 Sun
  const daysInMonth = new Date(year, month, 0).getDate()
  const cells = []
  for (let i = 0; i < startWeekday; i += 1) {
    cells.push({ day: 0, key: '', inMonth: false })
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    const key = `${year}-${pad(month)}-${pad(day)}`
    cells.push({ day, key, inMonth: true })
  }
  while (cells.length % 7 !== 0) {
    cells.push({ day: 0, key: '', inMonth: false })
  }
  const weeks = []
  for (let i = 0; i < cells.length; i += 7) {
    weeks.push(cells.slice(i, i + 7))
  }
  return weeks
}

function shiftMonth(year, month, delta) {
  const d = new Date(year, month - 1 + delta, 1)
  return { year: d.getFullYear(), month: d.getMonth() + 1 }
}

module.exports = {
  pad,
  toDateKey,
  parseDateKey,
  todayKey,
  addDays,
  formatDisplay,
  getMonthMatrix,
  shiftMonth
}
