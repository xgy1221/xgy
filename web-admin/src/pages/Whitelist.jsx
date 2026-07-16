import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { canEdit, canView } from '../auth/roles'
import { addWhitelist, listWhitelist, removeWhitelist } from '../data/store'

export default function Whitelist() {
  if (!canView('whitelist')) return <Navigate to="/" replace />
  const editable = canEdit('whitelist')
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ phone: '', parentName: '', note: '' })

  async function reload() {
    setLoading(true)
    setError('')
    try {
      setList(await listWhitelist())
    } catch (e) {
      setError(e.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    reload()
  }, [])

  async function onAdd() {
    if (!/^1\d{10}$/.test(form.phone || '')) return alert('请输入正确手机号')
    try {
      await addWhitelist(form)
      setForm({ phone: '', parentName: '', note: '' })
      await reload()
    } catch (e) {
      alert(e.message || '添加失败')
    }
  }

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>家长白名单</strong>
          <div className="muted">新家长手机号须先加入白名单才能登录；未开通将拒绝自注册</div>
        </div>
      </div>

      {editable && (
        <div className="form-grid" style={{ marginBottom: 16 }}>
          <div className="field">
            <label>手机号</label>
            <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </div>
          <div className="field">
            <label>家长称呼</label>
            <input value={form.parentName} onChange={(e) => setForm({ ...form, parentName: e.target.value })} />
          </div>
          <div className="field full">
            <label>备注</label>
            <input value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
          </div>
          <div className="field">
            <button className="btn" type="button" onClick={onAdd}>
              加入白名单
            </button>
          </div>
        </div>
      )}

      {error && <p className="muted" style={{ color: '#b45309' }}>{error}</p>}
      {loading ? (
        <p className="muted">加载中…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>手机号</th>
              <th>家长</th>
              <th>备注</th>
              {editable && <th />}
            </tr>
          </thead>
          <tbody>
            {list.map((w) => (
              <tr key={w.id}>
                <td>{w.phone}</td>
                <td>{w.parentName || '-'}</td>
                <td className="muted">{w.note || '-'}</td>
                {editable && (
                  <td>
                    <button
                      className="btn ghost"
                      type="button"
                      onClick={async () => {
                        if (!confirm('确认移除？')) return
                        await removeWhitelist(w.id)
                        await reload()
                      }}
                    >
                      移除
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
