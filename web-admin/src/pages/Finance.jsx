import { useMemo } from 'react'
import { Navigate } from 'react-router-dom'
import { financeMode, getUser } from '../auth/roles'
import { formatMoney, getFinanceSummary } from '../data/store'

export default function Finance() {
  const user = getUser()
  const mode = financeMode()
  if (mode === 'none') return <Navigate to="/" replace />

  const summary = useMemo(() => getFinanceSummary(user), [user])

  return (
    <div>
      <div className="panel" style={{ marginBottom: 14 }}>
        <strong>财务中心</strong>
        <div className="muted" style={{ marginTop: 6 }}>
          {mode === 'partner'
            ? '合伙人视角：本校区、本人渠道相关订单；分成=净收×分成比例（示例）。'
            : '管理员视角：全校区报名实收、退费、欠费与销售结构（教培常见财务看板）。'}
        </div>
      </div>

      <div className="grid-4">
        <div className="stat">
          <div className="label">签约额</div>
          <div className="value" style={{ fontSize: 22 }}>
            {formatMoney(summary.sales)}
          </div>
        </div>
        <div className="stat">
          <div className="label">实收</div>
          <div className="value" style={{ fontSize: 22 }}>
            {formatMoney(summary.received)}
          </div>
        </div>
        <div className="stat">
          <div className="label">退费</div>
          <div className="value" style={{ fontSize: 22 }}>
            {formatMoney(summary.refund)}
          </div>
        </div>
        <div className="stat">
          <div className="label">{mode === 'partner' ? `分成(${Math.round(summary.shareRatio * 100)}%)` : '待收'}</div>
          <div className="value" style={{ fontSize: 22 }}>
            {mode === 'partner' ? formatMoney(summary.share) : formatMoney(summary.receivable)}
          </div>
        </div>
      </div>

      <div className="split" style={{ marginBottom: 14 }}>
        <div className="panel">
          <strong>按校区</strong>
          <table className="table">
            <thead>
              <tr>
                <th>校区</th>
                <th>实收</th>
                <th>退费</th>
                <th>净收</th>
              </tr>
            </thead>
            <tbody>
              {summary.byCampus.map((r) => (
                <tr key={r.campus}>
                  <td>{r.campus}</td>
                  <td>{formatMoney(r.received)}</td>
                  <td>{formatMoney(r.refund)}</td>
                  <td>{formatMoney(r.net)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="panel">
          <strong>按教案产品</strong>
          <table className="table">
            <thead>
              <tr>
                <th>教案</th>
                <th>单数</th>
                <th>实收</th>
              </tr>
            </thead>
            <tbody>
              {summary.byPackage.map((r) => (
                <tr key={r.name}>
                  <td>{r.name}</td>
                  <td>{r.count}</td>
                  <td>{formatMoney(r.received)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="panel">
        <div className="toolbar">
          <div>
            <strong>报名订单 / 收款流水</strong>
            <div className="muted">对应教案售价与缴费状态；后续可接支付与退费审批</div>
          </div>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>日期</th>
              <th>学员</th>
              <th>教案</th>
              <th>校区</th>
              <th>签约</th>
              <th>实收</th>
              <th>退费</th>
              <th>状态</th>
              <th>渠道</th>
            </tr>
          </thead>
          <tbody>
            {summary.orders.map((o) => (
              <tr key={o.id}>
                <td>{o.createdAt}</td>
                <td>
                  {o.studentName}
                  <div className="muted">{o.parentPhone}</div>
                </td>
                <td>
                  {o.packageName}
                  <div className="muted">{o.lessonCount} 节</div>
                </td>
                <td>{o.campus}</td>
                <td>{formatMoney(o.amount)}</td>
                <td>{formatMoney(o.paidAmount)}</td>
                <td>{formatMoney(o.refundAmount)}</td>
                <td>
                  <span
                    className={`tag ${
                      o.status === '已缴费' ? 'ok' : o.status === '待缴费' ? 'warn' : ''
                    }`}
                  >
                    {o.status}
                  </span>
                </td>
                <td>{o.channel}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
