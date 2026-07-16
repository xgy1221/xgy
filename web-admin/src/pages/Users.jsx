import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { canView, getUser } from '../auth/roles'
import { listStaffAccounts } from '../data/store'

export default function Users() {
  const user = getUser()
  const [list, setList] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!canView('users')) return undefined
    let alive = true
    listStaffAccounts()
      .then((rows) => {
        if (alive) setList(rows)
      })
      .catch((e) => {
        if (alive) setError(e.message || '加载失败')
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  if (!canView('users')) return <Navigate to="/" replace />

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>账号权限</strong>
          <div className="muted">一人可多角色（如合伙人兼教务/老师）；数据来自后端用户表</div>
        </div>
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
              <th>角色</th>
              <th>校区</th>
              <th>状态</th>
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
                <td>{u.campus}</td>
                <td>
                  <span className="tag ok">{u.status}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <p className="muted" style={{ marginTop: 12 }}>
        小程序端：学生/老师/教务为主；合伙人与管理员登录小程序看轻量看板，深度配置与财务在本 Web 端。
      </p>
    </div>
  )
}
