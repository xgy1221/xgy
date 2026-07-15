import { useMemo, useState } from 'react'
import MonthCalendar from '../components/MonthCalendar'
import { canEdit, getUser } from '../auth/roles'
import {
  addStudentToClass,
  createClass,
  createLesson,
  getLessonDateMarks,
  getLessonsByDate,
  listCampuses,
  listClasses,
  listPackages,
  listStudents,
  listTeachers,
  removeStudentFromClass,
  todayKey
} from '../data/store'

export default function Courses() {
  const user = getUser()
  const editable = canEdit('courses')
  const [tab, setTab] = useState('classes')
  const [tick, setTick] = useState(0)

  const classes = useMemo(() => listClasses(user), [tick, user])
  const packages = useMemo(() => listPackages(user).filter((p) => p.status === '上架'), [tick, user])
  const teachers = useMemo(() => listTeachers(user), [tick, user])
  const students = useMemo(() => listStudents(user), [tick, user])
  const campuses = listCampuses(user)

  const [openClass, setOpenClass] = useState(false)
  const [manageId, setManageId] = useState('')
  const [pickStudentId, setPickStudentId] = useState('')
  const [classForm, setClassForm] = useState({
    name: '',
    packageId: '',
    teacherId: '',
    campus: '城南校区',
    room: ''
  })

  const [selectedDate, setSelectedDate] = useState(todayKey())
  const marks = useMemo(() => getLessonDateMarks(user), [tick, user])
  const dayLessons = useMemo(() => getLessonsByDate(selectedDate, user), [selectedDate, tick, user])
  const [lessonForm, setLessonForm] = useState({
    classId: '',
    teacherId: '',
    startTime: '16:00',
    endTime: '17:30',
    room: ''
  })

  const managing = classes.find((c) => c.id === manageId)

  function saveClass() {
    if (!classForm.name.trim()) return alert('请填写班级名')
    const res = createClass(classForm)
    if (res?.ok === false) return alert(res.message)
    setOpenClass(false)
    setTick((t) => t + 1)
  }

  function onPickClass(classId) {
    const cls = classes.find((c) => c.id === classId)
    setLessonForm((f) => ({
      ...f,
      classId,
      teacherId: cls?.teacherId || f.teacherId,
      room: cls?.room || f.room
    }))
  }

  function submitLesson() {
    if (!lessonForm.classId || !lessonForm.teacherId) return alert('请选择班级和老师')
    const res = createLesson({ date: selectedDate, ...lessonForm })
    if (!res.ok) return alert(res.message)
    setTick((t) => t + 1)
  }

  const groups = {}
  dayLessons.forEach((l) => {
    if (!groups[l.teacherName]) groups[l.teacherName] = []
    groups[l.teacherName].push(l)
  })

  return (
    <div>
      <div className="panel" style={{ marginBottom: 14, padding: '12px 16px' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className={`btn ${tab === 'classes' ? '' : 'ghost'}`} type="button" onClick={() => setTab('classes')}>
            班级
          </button>
          <button
            className={`btn ${tab === 'schedule' ? '' : 'ghost'}`}
            type="button"
            onClick={() => setTab('schedule')}
          >
            日历订课
          </button>
        </div>
        <div className="muted" style={{ marginTop: 8 }}>
          课程管理 = 班级花名册 + 日历排课指定老师（教培常见做法）
        </div>
      </div>

      {tab === 'classes' && (
        <div className="panel">
          <div className="toolbar">
            <div>
              <strong>班级</strong>
              <div className="muted">绑定教案与默认老师，维护固定班学员</div>
            </div>
            {editable && (
              <button
                className="btn"
                type="button"
                onClick={() => {
                  setClassForm({
                    name: '',
                    packageId: packages[0]?.id || '',
                    teacherId: teachers[0]?.id || '',
                    campus: user?.campus || '城南校区',
                    room: ''
                  })
                  setOpenClass(true)
                }}
              >
                创建班级
              </button>
            )}
          </div>
          <table className="table">
            <thead>
              <tr>
                <th>班级</th>
                <th>教案</th>
                <th>老师</th>
                <th>学员</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {classes.map((c) => (
                <tr key={c.id}>
                  <td>
                    <strong>{c.name}</strong>
                    <div className="muted">
                      {c.campus} · {c.room || '未定教室'}
                    </div>
                  </td>
                  <td>
                    {c.packageName}
                    <div className="muted">
                      {c.lessonCount} 节 · ¥{c.packagePrice}
                    </div>
                  </td>
                  <td>{c.teacherName}</td>
                  <td>
                    {c.studentCount} 人
                    <div className="muted">{c.students.map((s) => s.studentName).join('、') || '暂无'}</div>
                  </td>
                  <td>
                    <button className="btn ghost" type="button" onClick={() => setManageId(c.id)}>
                      {editable ? '管理学员' : '查看学员'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'schedule' && (
        <div className="split">
          <div>
            <MonthCalendar selected={selectedDate} marks={marks} onSelect={setSelectedDate} />
            {editable && (
              <div className="panel" style={{ marginTop: 16 }}>
                <strong>为该日订课</strong>
                <div className="muted" style={{ marginBottom: 10 }}>
                  {selectedDate}
                </div>
                <div className="form-grid" style={{ gridTemplateColumns: '1fr' }}>
                  <div className="field">
                    <label>班级</label>
                    <select value={lessonForm.classId} onChange={(e) => onPickClass(e.target.value)}>
                      <option value="">请选择</option>
                      {classes.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label>授课老师</label>
                    <select
                      value={lessonForm.teacherId}
                      onChange={(e) => setLessonForm({ ...lessonForm, teacherId: e.target.value })}
                    >
                      <option value="">请选择</option>
                      {teachers.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label>时间</label>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <input
                        value={lessonForm.startTime}
                        onChange={(e) => setLessonForm({ ...lessonForm, startTime: e.target.value })}
                      />
                      <input
                        value={lessonForm.endTime}
                        onChange={(e) => setLessonForm({ ...lessonForm, endTime: e.target.value })}
                      />
                    </div>
                  </div>
                  <div className="field">
                    <label>教室</label>
                    <input
                      value={lessonForm.room}
                      onChange={(e) => setLessonForm({ ...lessonForm, room: e.target.value })}
                    />
                  </div>
                </div>
                <button className="btn" type="button" style={{ width: '100%', marginTop: 12 }} onClick={submitLesson}>
                  确认订课
                </button>
              </div>
            )}
          </div>
          <div className="panel">
            <strong>{selectedDate} 老师有课</strong>
            <div className="muted" style={{ marginBottom: 12 }}>
              按老师汇总，便于教务/合伙人对课
            </div>
            {!dayLessons.length && <div className="muted">这天暂无订课</div>}
            {Object.keys(groups).map((teacher) => (
              <div key={teacher} style={{ marginBottom: 16 }}>
                <div style={{ fontWeight: 700, color: 'var(--brand)', marginBottom: 8 }}>老师 · {teacher}</div>
                {groups[teacher].map((l) => (
                  <div className="lesson-card" key={l.id}>
                    <strong>
                      {l.startTime}-{l.endTime} · {l.className}
                    </strong>
                    <div className="muted">
                      {l.packageName} · {l.campus} · {l.room || '-'} · {l.studentCount} 人
                    </div>
                  </div>
                ))}
              </div>
            ))}
          </div>
        </div>
      )}

      {openClass && (
        <div className="modal-mask">
          <div className="modal">
            <h3>创建班级</h3>
            <div className="form-grid">
              <div className="field full">
                <label>班级名称</label>
                <input
                  value={classForm.name}
                  onChange={(e) => setClassForm({ ...classForm, name: e.target.value })}
                />
              </div>
              <div className="field">
                <label>教案</label>
                <select
                  value={classForm.packageId}
                  onChange={(e) => setClassForm({ ...classForm, packageId: e.target.value })}
                >
                  {packages.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}（{p.lessonCount}节 / ¥{p.price}）
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>默认老师</label>
                <select
                  value={classForm.teacherId}
                  onChange={(e) => setClassForm({ ...classForm, teacherId: e.target.value })}
                >
                  {teachers.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>校区</label>
                <select
                  value={classForm.campus}
                  onChange={(e) => setClassForm({ ...classForm, campus: e.target.value })}
                >
                  {campuses.map((c) => (
                    <option key={c.id} value={c.name}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>教室</label>
                <input
                  value={classForm.room}
                  onChange={(e) => setClassForm({ ...classForm, room: e.target.value })}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn ghost" type="button" onClick={() => setOpenClass(false)}>
                取消
              </button>
              <button className="btn" type="button" onClick={saveClass}>
                创建
              </button>
            </div>
          </div>
        </div>
      )}

      {managing && (
        <div className="modal-mask">
          <div className="modal">
            <h3>
              {managing.name} · 学员 {managing.studentCount} 人
            </h3>
            {editable && (
              <div className="field" style={{ marginBottom: 12 }}>
                <label>添加学员</label>
                <div style={{ display: 'flex', gap: 8 }}>
                  <select
                    style={{ flex: 1 }}
                    value={pickStudentId}
                    onChange={(e) => setPickStudentId(e.target.value)}
                  >
                    <option value="">选择学员</option>
                    {students
                      .filter((s) => !(managing.studentIds || []).includes(s.id))
                      .map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.studentName} · {s.parentPhone}
                        </option>
                      ))}
                  </select>
                  <button
                    className="btn"
                    type="button"
                    onClick={() => {
                      const res = addStudentToClass(managing.id, pickStudentId)
                      if (!res.ok) return alert(res.message)
                      setPickStudentId('')
                      setTick((t) => t + 1)
                    }}
                  >
                    加入
                  </button>
                </div>
              </div>
            )}
            <table className="table">
              <thead>
                <tr>
                  <th>学员</th>
                  <th>家长手机</th>
                  {editable && <th />}
                </tr>
              </thead>
              <tbody>
                {managing.students.map((s) => (
                  <tr key={s.id}>
                    <td>{s.studentName}</td>
                    <td>{s.parentPhone}</td>
                    {editable && (
                      <td>
                        <button
                          className="btn ghost"
                          type="button"
                          onClick={() => {
                            removeStudentFromClass(managing.id, s.id)
                            setTick((t) => t + 1)
                          }}
                        >
                          移出
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="modal-actions">
              <button className="btn" type="button" onClick={() => setManageId('')}>
                关闭
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
