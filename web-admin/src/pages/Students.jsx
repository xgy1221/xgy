import { useEffect, useState } from 'react'
import { canEdit, getUser } from '../auth/roles'
import { archiveStudent, listCampuses, listStudents, upsertStudent } from '../data/store'

const empty = {
  studentName: '',
  parentPhone: '',
  parentName: '',
  grade: '',
  campus: '城南校区',
  remark: '',
  status: '在读'
}

export default function Students() {
  const user = getUser()
  const editable = canEdit('students')
  const [keyword, setKeyword] = useState('')
  const [students, setStudents] = useState([])
  const [campuses, setCampuses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  async function reload() {
    setLoading(true)
    setError('')
    try {
      const [s, c] = await Promise.all([listStudents(user), listCampuses(user)])
      setStudents(s)
      setCampuses(c)
    } catch (e) {
      setError(e.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    reload()
  }, [user?.phone])

  const filtered = students.filter((s) => {
    const kw = keyword.trim()
    if (!kw) return true
    return (
      (s.studentName || '').includes(kw) ||
      (s.parentPhone || '').includes(kw) ||
      (s.parentName || '').includes(kw)
    )
  })

  async function save() {
    if (!form.studentName.trim()) return alert('请填写学生姓名')
    if (!/^1\d{10}$/.test(form.parentPhone || '')) return alert('请填写正确家长手机号')
    setSaving(true)
    try {
      await upsertStudent(form)
      setOpen(false)
      setForm(empty)
      await reload()
    } catch (e) {
      alert(e.message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>学员管理</strong>
          <div className="muted">
            {editable ? '维护档案；同一家长手机号可挂多个孩子' : '只读查看学员（合伙人为本校区）'}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <input
            placeholder="搜姓名 / 手机号"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{ border: '1px solid var(--border)', borderRadius: 999, padding: '8px 14px' }}
          />
          {editable && (
            <button
              className="btn"
              type="button"
              onClick={() => {
                setForm({ ...empty, campus: user?.campus || campuses[0]?.name || '城南校区' })
                setOpen(true)
              }}
            >
              新增学员
            </button>
          )}
        </div>
      </div>

      {error && <p className="muted" style={{ color: '#b45309' }}>{error}</p>}
      {loading ? (
        <p className="muted">加载中…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>学生</th>
              <th>家长</th>
              <th>年级 / 校区</th>
              <th>状态</th>
              <th>备注</th>
              {editable && <th />}
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
                <td>
                  <span className="tag ok">{s.status || '在读'}</span>
                </td>
                <td className="muted">{s.remark || '-'}</td>
              {editable && (
                <td style={{ whiteSpace: 'nowrap' }}>
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
                  <button
                    className="btn ghost"
                    type="button"
                    onClick={async () => {
                      if (!confirm(`确认归档「${s.studentName}」？`)) return
                      try {
                        await archiveStudent(s.id)
                        await reload()
                      } catch (e) {
                        alert(e.message || '归档失败')
                      }
                    }}
                  >
                    归档
                  </button>
                </td>
              )}
              </tr>
            ))}
          </tbody>
        </table>
      )}

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
                <select value={form.campus} onChange={(e) => setForm({ ...form, campus: e.target.value })}>
                  {campuses.map((c) => (
                    <option key={c.id} value={c.name}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>状态</label>
                <select value={form.status || '在读'} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <option value="在读">在读</option>
                  <option value="停课">停课</option>
                  <option value="结业">结业</option>
                </select>
              </div>
              <div className="field full">
                <label>备注</label>
                <input value={form.remark} onChange={(e) => setForm({ ...form, remark: e.target.value })} />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn ghost" type="button" onClick={() => setOpen(false)}>
                取消
              </button>
              <button className="btn" type="button" disabled={saving} onClick={save}>
                {saving ? '保存中…' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
