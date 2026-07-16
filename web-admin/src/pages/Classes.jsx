import { useMemo, useState } from 'react'
import { getUser } from '../auth/roles'
import {
  addStudentToClass,
  createClass,
  listClasses,
  listPackages,
  listStudents,
  listTeachers,
  removeStudentFromClass
} from '../data/store'

const empty = {
  name: '',
  packageId: '',
  teacherId: '',
  room: '',
  studentIds: []
}

export default function Classes() {
  const user = getUser()
  const [tick, setTick] = useState(0)
  const classes = useMemo(() => listClasses(user), [tick, user])
  const packages = useMemo(() => listPackages(user).filter((p) => p.status === '上架'), [tick, user])
  const teachers = useMemo(() => listTeachers(user), [tick, user])
  const students = useMemo(() => listStudents(user), [tick, user])
  const [open, setOpen] = useState(false)
  const [manageId, setManageId] = useState('')
  const [form, setForm] = useState(empty)
  const [pickStudentId, setPickStudentId] = useState('')

  const managing = classes.find((c) => c.id === manageId)

  function saveClass() {
    if (!form.name.trim()) return alert('请填写班级名')
    if (!form.packageId) return alert('请选择教案')
    if (!form.teacherId) return alert('请选择默认老师')
    const res = createClass(form)
    if (res?.ok === false) return alert(res.message)
    setOpen(false)
    setForm(empty)
    setTick((t) => t + 1)
  }

  function addStudent() {
    if (!manageId || !pickStudentId) return
    const res = addStudentToClass(manageId, pickStudentId)
    if (!res.ok) return alert(res.message)
    setPickStudentId('')
    setTick((t) => t + 1)
  }

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>班级管理</strong>
          <div className="muted">创建班级 → 绑定教案与老师 → 添加学员（固定班花名册）</div>
        </div>
        <button
          className="btn"
          type="button"
          onClick={() => {
            setForm({
              ...empty,
              packageId: packages[0]?.id || '',
              teacherId: teachers[0]?.id || ''
            })
            setOpen(true)
          }}
        >
          创建班级
        </button>
      </div>

      <table className="table">
        <thead>
          <tr>
            <th>班级</th>
            <th>教案 / 课次</th>
            <th>默认老师</th>
            <th>学员</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {classes.map((c) => (
            <tr key={c.id}>
              <td>
                <strong>{c.name}</strong>
                <div className="muted">教室 {c.room || '-'}</div>
              </td>
              <td>
                {c.packageName}
                <div className="muted">含 {c.lessonCount} 节</div>
              </td>
              <td>{c.teacherName}</td>
              <td>
                {c.studentCount} 人
                <div className="muted">{c.students.map((s) => s.studentName).join('、') || '暂无'}</div>
              </td>
              <td>
                <button className="btn ghost" type="button" onClick={() => setManageId(c.id)}>
                  管理学员
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {open && (
        <div className="modal-mask">
          <div className="modal">
            <h3>创建班级</h3>
            <div className="form-grid">
              <div className="field full">
                <label>班级名称</label>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="field">
                <label>绑定教案</label>
                <select
                  value={form.packageId}
                  onChange={(e) => setForm({ ...form, packageId: e.target.value })}
                >
                  {packages.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}（{p.lessonCount}节）
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>默认授课老师</label>
                <select
                  value={form.teacherId}
                  onChange={(e) => setForm({ ...form, teacherId: e.target.value })}
                >
                  {teachers.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field full">
                <label>教室</label>
                <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn ghost" type="button" onClick={() => setOpen(false)}>
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
            <h3>管理学员 · {managing.name}</h3>
            <p className="muted">
              教案：{managing.packageName} · 老师：{managing.teacherName}
            </p>
            <div className="field" style={{ marginBottom: 12 }}>
              <label>添加学员</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <select
                  value={pickStudentId}
                  onChange={(e) => setPickStudentId(e.target.value)}
                  style={{ flex: 1 }}
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
                <button className="btn" type="button" onClick={addStudent}>
                  加入
                </button>
              </div>
            </div>
            <table className="table">
              <thead>
                <tr>
                  <th>学员</th>
                  <th>家长手机号</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {managing.students.map((s) => (
                  <tr key={s.id}>
                    <td>{s.studentName}</td>
                    <td>{s.parentPhone}</td>
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
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="modal-actions">
              <button className="btn" type="button" onClick={() => setManageId('')}>
                完成
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
