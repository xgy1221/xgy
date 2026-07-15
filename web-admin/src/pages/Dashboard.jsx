import { Link } from 'react-router-dom'
import { getDashboardStats, todayKey } from '../data/store'

export default function Dashboard() {
  const stats = getDashboardStats()

  return (
    <div>
      <div className="grid-4">
        <div className="stat">
          <div className="label">教案产品</div>
          <div className="value">{stats.packageCount}</div>
        </div>
        <div className="stat">
          <div className="label">学员档案</div>
          <div className="value">{stats.studentCount}</div>
        </div>
        <div className="stat">
          <div className="label">开班班级</div>
          <div className="value">{stats.classCount}</div>
        </div>
        <div className="stat">
          <div className="label">今日订课</div>
          <div className="value">{stats.todayLessonCount}</div>
        </div>
      </div>

      <div className="panel">
        <h3 style={{ marginTop: 0 }}>教务日常怎么做</h3>
        <ol className="muted" style={{ lineHeight: 1.9 }}>
          <li>
            <Link to="/packages">创建教案</Link>：设置名称、总课次数、售价（销售也会用）
          </li>
          <li>
            <Link to="/students">录入学员</Link>：家长手机号可挂多个孩子
          </li>
          <li>
            <Link to="/classes">创建班级</Link>：绑定教案与默认老师，再把学员加进班
          </li>
          <li>
            <Link to="/schedule">日历订课</Link>：选日期，为班级安排授课老师与时间
          </li>
        </ol>
        <p className="muted">今天：{todayKey()}。上课、临补、评价消课在微信小程序老师/家长端完成。</p>
      </div>
    </div>
  )
}
