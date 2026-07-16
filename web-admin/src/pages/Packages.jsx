import { useEffect, useState } from 'react'
import { canEdit } from '../auth/roles'
import { archivePackage, listPackages, upsertPackage } from '../data/store'

const empty = {
  name: '',
  subject: '数学',
  grade: '',
  lessonCount: 24,
  price: 0,
  outline: '',
  status: '上架'
}

export default function Packages() {
  const editable = canEdit('packages')
  const [packages, setPackages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  async function reload() {
    setLoading(true)
    setError('')
    try {
      setPackages(await listPackages())
    } catch (e) {
      setError(e.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    reload()
  }, [])

  async function save() {
    if (!(form.name || '').trim()) return alert('请填写教案名称')
    if (!Number(form.lessonCount)) return alert('请填写课次数')
    setSaving(true)
    try {
      await upsertPackage({
        ...form,
        lessonCount: Number(form.lessonCount),
        price: Number(form.price) || 0
      })
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
          <strong>教案管理</strong>
          <div className="muted">销售产品：包含多少节课、售价、适用年级（与小程序共用后端）</div>
        </div>
        {editable && (
          <button
            className="btn"
            type="button"
            onClick={() => {
              setForm(empty)
              setOpen(true)
            }}
          >
            新建教案
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
              <th>名称</th>
              <th>科类</th>
              <th>课次数</th>
              <th>售价</th>
              <th>状态</th>
              {editable && <th />}
            </tr>
          </thead>
          <tbody>
            {packages.map((p) => (
              <tr key={p.id}>
                <td>
                  <strong>{p.name}</strong>
                  <div className="muted">{p.grade}</div>
                  <div className="muted">{p.outline}</div>
                </td>
                <td>{p.subject}</td>
                <td>
                  <strong>{p.lessonCount}</strong> 节
                </td>
                <td>¥{p.price}</td>
                <td>
                  <span className={`tag ${p.status === '上架' ? 'ok' : 'warn'}`}>{p.status}</span>
                </td>
              {editable && (
                <td style={{ whiteSpace: 'nowrap' }}>
                  <button
                    className="btn ghost"
                    type="button"
                    onClick={() => {
                      setForm({ ...p })
                      setOpen(true)
                    }}
                  >
                    编辑
                  </button>
                  <button
                    className="btn ghost"
                    type="button"
                    onClick={async () => {
                      if (!confirm(`确认下架归档「${p.name}」？`)) return
                      try {
                        await archivePackage(p.id)
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
            <h3>{form.id ? '编辑教案' : '新建教案'}</h3>
            <div className="form-grid">
              <div className="field full">
                <label>教案名称</label>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="field">
                <label>科类</label>
                <input value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} />
              </div>
              <div className="field">
                <label>适用年级</label>
                <input value={form.grade} onChange={(e) => setForm({ ...form, grade: e.target.value })} />
              </div>
              <div className="field">
                <label>包含课次数</label>
                <input
                  type="number"
                  value={form.lessonCount}
                  onChange={(e) => setForm({ ...form, lessonCount: e.target.value })}
                />
              </div>
              <div className="field">
                <label>售价</label>
                <input
                  type="number"
                  value={form.price}
                  onChange={(e) => setForm({ ...form, price: e.target.value })}
                />
              </div>
              <div className="field">
                <label>状态</label>
                <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <option value="上架">上架</option>
                  <option value="下架">下架</option>
                </select>
              </div>
              <div className="field full">
                <label>大纲说明</label>
                <textarea
                  rows={3}
                  value={form.outline}
                  onChange={(e) => setForm({ ...form, outline: e.target.value })}
                />
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
