import { Link } from 'react-router-dom'
import { getUser } from '../auth/roles'
import { formatMoney, getDashboardStats, todayKey } from '../data/store'

export default function Dashboard() {
  const user = getUser()
  const stats = getDashboardStats(user)
  const fin = stats.finance

  return (
    <div>
      <div className="grid-4">
        <div className="stat">
          <div className="label">学员</div>
          <div className="value">{stats.studentCount}</div>
        </div>
        <div className="stat">
          <div className="label">教师</div>
          <div className="value">{stats.teacherCount}</div>
        </div>
        <div className="stat">
          <div className="label">班级</div>
          <div className="value">{stats.classCount}</div>
        </div>
        <div className="stat">
          <div className="label">今日订课</div>
          <div className="value">{stats.todayLessonCount}</div>
        </div>
      </div>

      {fin && (
        <div className="grid-4" style={{ marginTop: 0 }}>
          <div className="stat">
            <div className="label">实收</div>
            <div className="value" style={{ fontSize: 22 }}>
              {formatMoney(fin.received)}
            </div>
          </div>
          <div className="stat">
            <div className="label">退费</div>
            <div className="value" style={{ fontSize: 22 }}>
              {formatMoney(fin.refund)}
            </div>
          </div>
          <div className="stat">
            <div className="label">净收</div>
            <div className="value" style={{ fontSize: 22 }}>
              {formatMoney(fin.net)}
            </div>
          </div>
          <div className="stat">
            <div className="label">{user.role === 'partner' ? '我的分成' : '待收账款'}</div>
            <div className="value" style={{ fontSize: 22 }}>
              {user.role === 'partner' ? formatMoney(fin.share) : formatMoney(fin.receivable)}
            </div>
          </div>
        </div>
      )}

      <div className="panel">
        <h3 style={{ marginTop: 0 }}>角色能做什么</h3>
        {user.role === 'academic' && (
          <ol className="muted" style={{ lineHeight: 1.9 }}>
            <li>
              <Link to="/packages">教案管理</Link>：定价、课次数（也相当于销售产品）
            </li>
            <li>
              <Link to="/students">学员</Link> / <Link to="/teachers">教师</Link> 档案维护
            </li>
            <li>
              <Link to="/courses">课程管理</Link>：建班、加学员、日历订课指定老师
            </li>
            <li>财务中心不对教务开放（避免干扰收款与分成口径）</li>
          </ol>
        )}
        {user.role === 'partner' && (
          <ol className="muted" style={{ lineHeight: 1.9 }}>
            <li>
              <Link to="/finance">财务中心</Link>：本校区实收/退费/净收与分成
            </li>
            <li>学员、教师、课程、教案可查看，默认只读</li>
            <li>细排课与改档案由教务处理；小程序可看轻量业绩看板</li>
          </ol>
        )}
        {user.role === 'admin' && (
          <ol className="muted" style={{ lineHeight: 1.9 }}>
            <li>全部业务模块可编辑</li>
            <li>
              <Link to="/finance">财务中心</Link>：全校区实收、退费、欠费与教案销售结构
            </li>
            <li>
              <Link to="/users">账号权限</Link>：教务/合伙/老师等多角色授权
            </li>
          </ol>
        )}
        <p className="muted">今天：{todayKey()} · 教案数 {stats.packageCount}</p>
      </div>
    </div>
  )
}
