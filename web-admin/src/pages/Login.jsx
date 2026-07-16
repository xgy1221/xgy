import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DEMO_ACCOUNTS, ROLE_LABEL, loginWithSms } from '../auth/roles'

export default function Login() {
  const navigate = useNavigate()
  const [phone, setPhone] = useState('13800000003')
  const [smsCode, setSmsCode] = useState('123456')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function submit(nextPhone = phone, campus = '') {
    setLoading(true)
    setError('')
    try {
      await loginWithSms(nextPhone, smsCode || '123456', campus)
      navigate('/')
    } catch (e) {
      setError(e.message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card" style={{ width: 'min(560px, 100%)' }}>
        <h1>学管云 Web 管理端</h1>
        <p>与小程序共用同一后端（JWT + MySQL）。教务 / 合伙 / 管理员在此维护与查看机构数据。</p>

        <div className="form-grid" style={{ marginBottom: 16 }}>
          <div className="field">
            <label>手机号</label>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="员工手机号" />
          </div>
          <div className="field">
            <label>短信验证码</label>
            <input value={smsCode} onChange={(e) => setSmsCode(e.target.value)} placeholder="演示码 123456" />
          </div>
        </div>
        {error && (
          <div className="muted" style={{ color: '#b45309', marginBottom: 12 }}>
            {error}
          </div>
        )}
        <button className="btn" type="button" style={{ width: '100%', marginBottom: 18 }} disabled={loading} onClick={() => submit()}>
          {loading ? '登录中…' : '登录'}
        </button>

        <div className="muted" style={{ marginBottom: 8 }}>
          演示账号（点击填入并登录，验证码默认 123456）
        </div>
        <div style={{ display: 'grid', gap: 12 }}>
          {DEMO_ACCOUNTS.map((acc) => (
            <button
              key={acc.phone}
              type="button"
              className="btn ghost"
              disabled={loading}
              style={{
                width: '100%',
                textAlign: 'left',
                borderRadius: 14,
                padding: '14px 16px',
                display: 'block'
              }}
              onClick={() => {
                setPhone(acc.phone)
                setSmsCode('123456')
                submit(acc.phone, acc.campus)
              }}
            >
              <div style={{ fontWeight: 700, marginBottom: 4 }}>
                {acc.name} · {ROLE_LABEL[acc.role]}
              </div>
              <div className="muted">
                {acc.orgName} · {acc.phone} · {acc.campus}
              </div>
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
