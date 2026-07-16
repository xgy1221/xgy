import { useEffect, useState } from 'react'
import { canEdit, getUser } from '../auth/roles'
import { listCampuses, listTeachers, upsertTeacher } from '../data/store'

const empty = {
  name: '',
  phone: '',
  title: '',
  subjects: '',
  campus: '城南校区',
  status: '在职'
}

export default function Teachers() {
  const user = getUser()
  const editable = canEdit('teachers')
  const [teachers, setTeachers] = useState([])
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
      const [t, c] = await Promise.all([listTeachers(user), listCampuses(user)])
      setTeachers(t)
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

  async function save() {
    if (!form.name.trim()) return alert('请填写教师姓名')
    setSaving(true)
    try {
      await upsertTeacher(form)
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
          <strong>教师管理</strong>
          <div className="muted">{editable ? '维护教师档案与授课科类' : '只读查看教师信息'}</div>
        </div>
        {editable && (
          <button
            className="btn"
            type="button"
            onClick={() => {
              setForm({ ...empty, campus: user?.campus || campuses[0]?.name || '城南校区' })
              setOpen(true)
            }}
          >
            新增教师
          </button>
        )}
      </div>

      {error && <p className="muted" style={{ color: '#b45309' }}>{error}</p>}
      {loading ? (
        <p className="muted">加载中…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>姓名</th>
              <th>手机号</th>
              <th>职称 / 科类</th>
              <th>校区</th>
              <th>状态</th>
              {editable && <th />}
            </tr>
          </thead>
          <tbody>
            {teachers.map((t) => (
              <tr key={t.id}>
                <td>
                  <strong>{t.name}</strong>
                </td>
                <td>{t.phone}</td>
                <td>
                  {t.title}
                  <div className="muted">{t.subjects}</div>
                </td>
                <td>{t.campus}</td>
                <td>
                  <span className={`tag ${t.status === '在职' ? 'ok' : 'warn'}`}>{t.status}</span>
                </td>
                {editable && (
                  <td>
                    <button
                      className="btn ghost"
                      type="button"
                      onClick={() => {
                        setForm(t)
                        setOpen(true)
                      }}
                    >
                      编辑
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
            <h3>{form.id ? '编辑教师' : '新增教师'}</h3>
            <div className="form-grid">
              <div className="field">
                <label>姓名</label>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="field">
                <label>手机号</label>
                <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </div>
              <div className="field">
                <label>职称</label>
                <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
              </div>
              <div className="field">
                <label>科类</label>
                <input value={form.subjects} onChange={(e) => setForm({ ...form, subjects: e.target.value })} />
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
                <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <option value="在职">在职</option>
                  <option value="停用">停用</option>
                </select>
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
