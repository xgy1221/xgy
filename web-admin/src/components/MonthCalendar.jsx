import { useMemo, useState } from 'react'

function pad(n) {
  return n < 10 ? `0${n}` : `${n}`
}

function matrix(year, month) {
  const first = new Date(year, month - 1, 1)
  const start = first.getDay()
  const days = new Date(year, month, 0).getDate()
  const cells = []
  for (let i = 0; i < start; i += 1) cells.push(null)
  for (let d = 1; d <= days; d += 1) {
    cells.push({ day: d, key: `${year}-${pad(month)}-${pad(d)}` })
  }
  while (cells.length % 7 !== 0) cells.push(null)
  const weeks = []
  for (let i = 0; i < cells.length; i += 7) weeks.push(cells.slice(i, i + 7))
  return weeks
}

export default function MonthCalendar({ selected, marks = [], onSelect }) {
  const today = useMemo(() => {
    const d = new Date()
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }, [])

  const init = selected || today
  const [year, setYear] = useState(Number(init.slice(0, 4)))
  const [month, setMonth] = useState(Number(init.slice(5, 7)))

  const markMap = useMemo(() => {
    const map = {}
    marks.forEach((m) => {
      map[m.date] = m.state
    })
    return map
  }, [marks])

  const weeks = matrix(year, month)

  function shift(delta) {
    const d = new Date(year, month - 1 + delta, 1)
    setYear(d.getFullYear())
    setMonth(d.getMonth() + 1)
  }

  return (
    <div className="cal">
      <div className="cal-head">
        <button type="button" onClick={() => shift(-1)}>
          ‹
        </button>
        <strong>
          {year}年{month}月
        </strong>
        <button type="button" onClick={() => shift(1)}>
          ›
        </button>
      </div>
      <div className="week">
        {['日', '一', '二', '三', '四', '五', '六'].map((w) => (
          <span key={w}>{w}</span>
        ))}
      </div>
      {weeks.map((week, wi) => (
        <div className="days-row" key={wi}>
          {week.map((cell, ci) => {
            if (!cell) return <div className="day empty" key={`e-${ci}`} />
            const state = markMap[cell.key]
            const cls = [
              'day',
              cell.key === today ? 'today' : '',
              cell.key === selected ? 'selected' : '',
              state ? `mark-${state}` : ''
            ]
              .filter(Boolean)
              .join(' ')
            return (
              <div key={cell.key} className={cls} onClick={() => onSelect?.(cell.key)}>
                {cell.day}
              </div>
            )
          })}
        </div>
      ))}
      <div className="legend">
        <span>
          <i className="dot finished" />
          已上课
        </span>
        <span>
          <i className="dot upcoming" />
          未上课 / 已订课
        </span>
      </div>
    </div>
  )
}
