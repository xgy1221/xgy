import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { canView } from '../auth/roles'
import { listAuditLogs } from '../data/store'

export default function Audit() {
  if (!canView('audit')) return <Navigate to="/" replace />
  const [list, setList] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listAuditLogs()
      .then(setList)
      .catch((e) => setError(e.message || '加载失败'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>操作审计</strong>
          <div className="muted">登录、授权、录单等敏感操作留痕（最近 100 条）</div>
        </div>
      </div>
      {error && <p className="muted" style={{ color: '#b45309' }}>{error}</p>}
      {loading ? (
        <p className="muted">加载中…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>时间</th>
              <th>动作</th>
              <th>操作人</th>
              <th>对象</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            {list.map((a) => (
              <tr key={a.id}>
                <td>{a.createdAt ? String(a.createdAt).replace('T', ' ').slice(0, 19) : '-'}</td>
                <td>{a.action}</td>
                <td>{a.phone || '-'}</td>
                <td className="muted">
                  {a.targetType || '-'}
                  {a.targetId ? `#${a.targetId}` : ''}
                </td>
                <td className="muted">{a.detail || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
