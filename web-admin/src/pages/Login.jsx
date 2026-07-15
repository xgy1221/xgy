import { useNavigate } from 'react-router-dom'
import { DEMO_ACCOUNTS, ROLE_LABEL, setUser } from '../auth/roles'

export default function Login() {
  const navigate = useNavigate()

  function enter(account) {
    setUser(account)
    navigate('/')
  }

  return (
    <div className="login-page">
      <div className="login-card" style={{ width: 'min(560px, 100%)' }}>
        <h1>学管云 Web 管理端</h1>
        <p>教务、合伙人、管理员共用后台；菜单与财务按角色授权。小程序侧重学生/老师/教务上课现场。</p>

        <div style={{ display: 'grid', gap: 12 }}>
          {DEMO_ACCOUNTS.map((acc) => (
            <button
              key={acc.phone}
              type="button"
              className="btn ghost"
              style={{
                width: '100%',
                textAlign: 'left',
                borderRadius: 14,
                padding: '14px 16px',
                display: 'block'
              }}
              onClick={() => enter(acc)}
            >
              <div style={{ fontWeight: 700, marginBottom: 4 }}>
                {acc.name} · {ROLE_LABEL[acc.role]}
              </div>
              <div className="muted">{acc.phone} · {acc.campus}</div>
              <div className="muted" style={{ marginTop: 4 }}>
                {acc.desc}
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
