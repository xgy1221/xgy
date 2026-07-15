import { useMemo, useState } from 'react'
import MonthCalendar from '../components/MonthCalendar'
import {
  createLesson,
  getLessonDateMarks,
  getLessonsByDate,
  listClasses,
  listTeachers,
  todayKey
} from '../data/store'

export default function Schedule() {
  const [tick, setTick] = useState(0)
  const [selectedDate, setSelectedDate] = useState(todayKey())
  const classes = useMemo(() => listClasses(), [tick])
  const teachers = useMemo(() => listTeachers(), [])
  const marks = useMemo(() => getLessonDateMarks(), [tick])
  const dayLessons = useMemo(() => getLessonsByDate(selectedDate), [selectedDate, tick])

  const [form, setForm] = useState({
    classId: '',
    teacherId: '',
    startTime: '16:00',
    endTime: '17:30',
    room: ''
  })

  function onPickClass(classId) {
    const cls = classes.find((c) => c.id === classId)
    setForm((f) => ({
      ...f,
      classId,
      teacherId: cls?.teacherId || f.teacherId,
      room: cls?.room || f.room
    }))
  }

  function submit() {
    if (!selectedDate) return alert('请先选择日期')
    if (!form.classId) return alert('请选择班级')
    if (!form.teacherId) return alert('请选择授课老师')
    const res = createLesson({
      date: selectedDate,
      ...form
    })
    if (!res.ok) return alert(res.message)
    setTick((t) => t + 1)
  }

  const groups = {}
  dayLessons.forEach((l) => {
    if (!groups[l.teacherName]) groups[l.teacherName] = []
    groups[l.teacherName].push(l)
  })

  return (
    <div className="split">
      <div>
        <MonthCalendar selected={selectedDate} marks={marks} onSelect={setSelectedDate} />
        <div className="panel" style={{ marginTop: 16 }}>
          <strong>为该日订课</strong>
          <div className="muted" style={{ marginBottom: 10 }}>
            当前日期：{selectedDate}
          </div>
          <div className="form-grid" style={{ gridTemplateColumns: '1fr' }}>
            <div className="field">
              <label>班级</label>
              <select value={form.classId} onChange={(e) => onPickClass(e.target.value)}>
                <option value="">请选择班级</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} · {c.packageName}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>授课老师</label>
              <select
                value={form.teacherId}
                onChange={(e) => setForm({ ...form, teacherId: e.target.value })}
              >
                <option value="">请选择老师</option>
                {teachers.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>开始时间</label>
              <input
                value={form.startTime}
                onChange={(e) => setForm({ ...form, startTime: e.target.value })}
              />
            </div>
            <div className="field">
              <label>结束时间</label>
              <input value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
            </div>
            <div className="field">
              <label>教室</label>
              <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} />
            </div>
          </div>
          <button className="btn" type="button" style={{ marginTop: 12, width: '100%' }} onClick={submit}>
            确认订课
          </button>
        </div>
      </div>

      <div className="panel">
        <div className="toolbar">
          <div>
            <strong>{selectedDate} 课次</strong>
            <div className="muted">按老师归类，方便教务一眼看谁有课</div>
          </div>
        </div>

        {!dayLessons.length && <div className="muted">这天还没有订课，左侧选班后可安排。</div>}

        {Object.keys(groups).map((teacher) => (
          <div key={teacher} style={{ marginBottom: 18 }}>
            <div style={{ fontWeight: 700, marginBottom: 8, color: 'var(--brand)' }}>老师 · {teacher}</div>
            {groups[teacher].map((l) => (
              <div className="lesson-card" key={l.id}>
                <strong>
                  {l.startTime}-{l.endTime} · {l.className}
                </strong>
                <div className="muted">
                  {l.packageName} · 教室 {l.room || '-'} · 班内 {l.studentCount} 人
                </div>
                <div style={{ marginTop: 6 }}>
                  <span className={`tag ${l.status === 'finished' ? 'ok' : 'warn'}`}>
                    {l.status === 'finished' ? '已上课' : '已订课'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}
