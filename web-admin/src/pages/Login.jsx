import { useNavigate } from 'react-router-dom'

export default function Login() {
  const navigate = useNavigate()

  function enter() {
    localStorage.setItem(
      'xgy_web_user',
      JSON.stringify({ name: '赵教务', role: 'academic', phone: '13800000003' })
    )
    navigate('/')
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>学管云管理端</h1>
        <p>教务也兼销售：维护教案（几节课）、创建班级加学员、在日历上订课并指定老师。上课互评在小程序完成。</p>
        <button className="btn" type="button" onClick={enter} style={{ width: '100%' }}>
          教务身份进入
        </button>
        <p className="muted" style={{ marginTop: 14 }}>
          演示免密登录。正式环境可接账号权限与后端 API。
        </p>
      </div>
    </div>
  )
}
