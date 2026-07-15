import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { NAV_ITEMS, ROLE_LABEL, canView, clearUser, getUser } from '../auth/roles'

export default function Layout() {
  const navigate = useNavigate()
  const user = getUser()

  const links = NAV_ITEMS.filter((l) => !l.module || canView(l.module))

  function logout() {
    clearUser()
    navigate('/login')
  }

  return (
    <div className="shell">
      <aside className="sider">
        <div className="brand">学管云</div>
        <div className="brand-sub">Web 管理端 · {ROLE_LABEL[user?.role] || '-'}</div>
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
            <h2>{ROLE_LABEL[user?.role] || '管理'}工作台</h2>
            <p className="sub">
              {user?.role === 'academic' && '维护学员、老师、教案与班级订课'}
              {user?.role === 'partner' && '查看本校区业绩与分成，业务数据只读'}
              {user?.role === 'admin' && '全校配置、账号权限与完整财务'}
            </p>
          </div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <span className="muted">
              {user?.name} · {user?.campus}
            </span>
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
