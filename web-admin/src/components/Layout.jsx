import { NavLink, Outlet, useNavigate } from 'react-router-dom'

const links = [
  { to: '/', label: '工作台', end: true },
  { to: '/packages', label: '教案管理' },
  { to: '/students', label: '学员管理' },
  { to: '/classes', label: '班级管理' },
  { to: '/schedule', label: '日历订课' }
]

export default function Layout() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('xgy_web_user') || 'null')

  function logout() {
    localStorage.removeItem('xgy_web_user')
    navigate('/login')
  }

  return (
    <div className="shell">
      <aside className="sider">
        <div className="brand">学管云</div>
        <div className="brand-sub">教务 / 销售 Web 管理端</div>
        <nav className="nav">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              {l.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <section className="main">
        <div className="topbar">
          <div>
            <h2>教务运营台</h2>
            <p className="sub">创建教案 · 建班加学员 · 日历订课（小程序端负责家长/老师上课）</p>
          </div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <span className="muted">{user?.name || '教务'} · {user?.role || 'academic'}</span>
            <button className="btn ghost" type="button" onClick={logout}>
              退出
            </button>
          </div>
        </div>
        <Outlet />
      </section>
    </div>
  )
}
