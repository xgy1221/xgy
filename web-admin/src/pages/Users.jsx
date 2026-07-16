import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { canEdit, canView, getUser } from '../auth/roles'
import { grantRole, listStaffAccounts, revokeRole } from '../data/store'

const ROLE_OPTIONS = [
  { value: 'ACADEMIC', label: '教务' },
  { value: 'TEACHER', label: '老师' },
  { value: 'PARTNER', label: '合伙人' },
  { value: 'ADMIN', label: '管理员' }
]

export default function Users() {
  const user = getUser()
  if (!canView('users')) return <Navigate to="/" replace />
  const editable = canEdit('users')
  const [list, setList] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ phone: '', name: '', role: 'ACADEMIC' })

  async function reload() {
    setLoading(true)
    setError('')
    try {
      setList(await listStaffAccounts())
    } catch (e) {
      setError(e.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    reload()
  }, [])

  async function onGrant() {
    if (!/^1\d{10}$/.test(form.phone || '')) return alert('请输入正确手机号')
    try {
      await grantRole(form)
      setForm({ phone: '', name: '', role: 'ACADEMIC' })
      await reload()
    } catch (e) {
      alert(e.message || '授权失败')
    }
  }

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>账号权限</strong>
          <div className="muted">一人可多角色；授权后对方用同一手机号登录即可切换角色</div>
        </div>
      </div>

      {editable && (
        <div className="form-grid" style={{ marginBottom: 16 }}>
          <div className="field">
            <label>手机号</label>
            <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </div>
          <div className="field">
            <label>姓名</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div className="field">
            <label>角色</label>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              {ROLE_OPTIONS.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <button className="btn" type="button" onClick={onGrant}>
              授予角色
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
              <th>姓名</th>
              <th>手机号</th>
              <th>角色</th>
              <th>状态</th>
              {editable && <th />}
            </tr>
          </thead>
          <tbody>
            {list.map((u) => (
              <tr key={u.id}>
                <td>
                  <strong>{u.name}</strong>
                  {u.phone === user?.phone && (
                    <span className="tag ok" style={{ marginLeft: 8 }}>
                      当前
                    </span>
                  )}
                </td>
                <td>{u.phone}</td>
                <td>{u.roles.join(' / ')}</td>
                <td>
                  <span className="tag ok">{u.status}</span>
                </td>
                {editable && (
                  <td>
                    <select
                      defaultValue=""
                      onChange={async (e) => {
                        const role = e.target.value
                        if (!role) return
                        if (!confirm(`确认撤销 ${role}？`)) {
                          e.target.value = ''
                          return
                        }
                        try {
                          await revokeRole(u.id, role)
                          await reload()
                        } catch (err) {
                          alert(err.message || '撤权失败')
                        }
                        e.target.value = ''
                      }}
                    >
                      <option value="">撤权…</option>
                      {(u.rawRoles || []).map((role) => (
                        <option key={role} value={role}>
                          {ROLE_OPTIONS.find((r) => r.value === role)?.label || role}
                        </option>
                      ))}
                    </select>
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
