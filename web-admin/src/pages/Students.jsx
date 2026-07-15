import { useMemo, useState } from 'react'
import { listStudents, upsertStudent } from '../data/store'

const empty = {
  studentName: '',
  parentPhone: '',
  parentName: '',
  grade: '',
  campus: '城南校区',
  remark: ''
}

export default function Students() {
  const [tick, setTick] = useState(0)
  const [keyword, setKeyword] = useState('')
  const students = useMemo(() => listStudents(), [tick])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)

  const filtered = students.filter((s) => {
    const kw = keyword.trim()
    if (!kw) return true
    return (
      s.studentName.includes(kw) ||
      s.parentPhone.includes(kw) ||
      (s.parentName || '').includes(kw)
    )
  })

  function save() {
    if (!form.studentName.trim()) return alert('请填写学生姓名')
    if (!/^1\d{10}$/.test(form.parentPhone || '')) return alert('请填写正确家长手机号')
    upsertStudent(form)
    setOpen(false)
    setForm(empty)
    setTick((t) => t + 1)
  }

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>学员档案</strong>
          <div className="muted">支持同一家长手机号挂多个孩子</div>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <input
            placeholder="搜姓名 / 手机号"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{ border: '1px solid var(--border)', borderRadius: 999, padding: '8px 14px' }}
          />
          <button
            className="btn"
            type="button"
            onClick={() => {
              setForm(empty)
              setOpen(true)
            }}
          >
            新增学员
          </button>
        </div>
      </div>

      <table className="table">
        <thead>
          <tr>
            <th>学生</th>
            <th>家长</th>
            <th>年级 / 校区</th>
            <th>备注</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {filtered.map((s) => (
            <tr key={s.id}>
              <td>
                <strong>{s.studentName}</strong>
              </td>
              <td>
                {s.parentName || '家长'}
                <div className="muted">{s.parentPhone}</div>
              </td>
              <td>
                {s.grade}
                <div className="muted">{s.campus}</div>
              </td>
              <td className="muted">{s.remark || '-'}</td>
              <td>
                <button
                  className="btn ghost"
                  type="button"
                  onClick={() => {
                    setForm(s)
                    setOpen(true)
                  }}
                >
                  编辑
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {open && (
        <div className="modal-mask">
          <div className="modal">
            <h3>{form.id ? '编辑学员' : '新增学员'}</h3>
            <div className="form-grid">
              <div className="field">
                <label>学生姓名</label>
                <input
                  value={form.studentName}
                  onChange={(e) => setForm({ ...form, studentName: e.target.value })}
                />
              </div>
              <div className="field">
                <label>年级</label>
                <input value={form.grade} onChange={(e) => setForm({ ...form, grade: e.target.value })} />
              </div>
              <div className="field">
                <label>家长手机号</label>
                <input
                  value={form.parentPhone}
                  onChange={(e) => setForm({ ...form, parentPhone: e.target.value })}
                />
              </div>
              <div className="field">
                <label>家长称呼</label>
                <input
                  value={form.parentName}
                  onChange={(e) => setForm({ ...form, parentName: e.target.value })}
                />
              </div>
              <div className="field">
                <label>校区</label>
                <input value={form.campus} onChange={(e) => setForm({ ...form, campus: e.target.value })} />
              </div>
              <div className="field">
                <label>备注</label>
                <input value={form.remark} onChange={(e) => setForm({ ...form, remark: e.target.value })} />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn ghost" type="button" onClick={() => setOpen(false)}>
                取消
              </button>
              <button className="btn" type="button" onClick={save}>
                保存
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
