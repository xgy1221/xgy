import { useEffect, useState } from 'react'
import { canEdit, getUser } from '../auth/roles'
import { createOrder, listEnrollments, listPackages, listStudents, upsertEnrollment } from '../data/store'

export default function Enrollments() {
  const user = getUser()
  const editable = canEdit('enrollments')
  const [list, setList] = useState([])
  const [students, setStudents] = useState([])
  const [packages, setPackages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({
    studentId: '',
    packageId: '',
    remainLessons: '',
    totalLessons: '',
    status: '学习中',
    alsoOrder: true,
    amountPaid: ''
  })

  async function reload() {
    setLoading(true)
    setError('')
    try {
      const [e, s, p] = await Promise.all([listEnrollments(), listStudents(user), listPackages()])
      setList(e)
      setStudents(s)
      setPackages(p.filter((x) => x.status === '上架'))
    } catch (err) {
      setError(err.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    reload()
  }, [])

  async function save() {
    if (!form.studentId || !form.packageId) return alert('请选择学员与教案')
    try {
      await upsertEnrollment(form)
      if (form.alsoOrder) {
        const pkg = packages.find((p) => String(p.id) === String(form.packageId))
        const stu = students.find((s) => String(s.id) === String(form.studentId))
        await createOrder({
          studentId: form.studentId,
          packageId: form.packageId,
          campus: stu?.campus || '',
          amountTotal: pkg?.price || 0,
          amountPaid: Number(form.amountPaid) || 0,
          createEnrollment: false
        })
      }
      setOpen(false)
      await reload()
    } catch (err) {
      alert(err.message || '保存失败')
    }
  }

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>报读管理</strong>
          <div className="muted">学员购买/代录的教案课时；消课后剩余课时自动扣减</div>
        </div>
        {editable && (
          <button
            className="btn"
            type="button"
            onClick={() => {
              setForm({
                studentId: students[0]?.id || '',
                packageId: packages[0]?.id || '',
                remainLessons: packages[0]?.lessonCount || '',
                totalLessons: packages[0]?.lessonCount || '',
                status: '学习中',
                alsoOrder: true,
                amountPaid: packages[0]?.price || ''
              })
              setOpen(true)
            }}
          >
            代录报读
          </button>
        )}
      </div>
      {error && <p className="muted" style={{ color: '#b45309' }}>{error}</p>}
      {loading ? (
        <p className="muted">加载中…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>学员</th>
              <th>教案</th>
              <th>剩余 / 总课次</th>
              <th>进度</th>
              <th>状态</th>
              <th>来源</th>
            </tr>
          </thead>
          <tbody>
            {list.map((e) => (
              <tr key={e.id}>
                <td>
                  <strong>{e.studentName}</strong>
                  <div className="muted">{e.parentPhone}</div>
                </td>
                <td>{e.packageName}</td>
                <td>
                  {e.remainLessons} / {e.totalLessons}
                </td>
                <td>{e.progress != null ? `${e.progress}%` : '-'}</td>
                <td>
                  <span className="tag ok">{e.status}</span>
                </td>
                <td className="muted">{e.source || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {open && (
        <div className="modal-mask">
          <div className="modal">
            <h3>代录报读</h3>
            <div className="form-grid">
              <div className="field">
                <label>学员</label>
                <select value={form.studentId} onChange={(e) => setForm({ ...form, studentId: e.target.value })}>
                  {students.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.studentName} · {s.parentPhone}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>教案</label>
                <select
                  value={form.packageId}
                  onChange={(e) => {
                    const pkg = packages.find((p) => String(p.id) === String(e.target.value))
                    setForm({
                      ...form,
                      packageId: e.target.value,
                      totalLessons: pkg?.lessonCount || form.totalLessons,
                      remainLessons: pkg?.lessonCount || form.remainLessons
                    })
                  }}
                >
                  {packages.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}（{p.lessonCount}节）
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>总课次</label>
                <input
                  type="number"
                  value={form.totalLessons}
                  onChange={(e) => setForm({ ...form, totalLessons: e.target.value })}
                />
              </div>
              <div className="field">
                <label>剩余课次</label>
                <input
                  type="number"
                  value={form.remainLessons}
                  onChange={(e) => setForm({ ...form, remainLessons: e.target.value })}
                />
              </div>
              <div className="field">
                <label>状态</label>
                <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <option value="学习中">学习中</option>
                  <option value="暂停">暂停</option>
                  <option value="已结业">已结业</option>
                </select>
              </div>
              <div className="field">
                <label>
                  <input
                    type="checkbox"
                    checked={!!form.alsoOrder}
                    onChange={(e) => setForm({ ...form, alsoOrder: e.target.checked })}
                  />{' '}
                  同步录订单
                </label>
                <input
                  type="number"
                  placeholder="实收金额"
                  disabled={!form.alsoOrder}
                  value={form.amountPaid}
                  onChange={(e) => setForm({ ...form, amountPaid: e.target.value })}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn ghost" type="button" onClick={() => setOpen(false)}>
                取消
              </button>
              <button className="btn" type="button" onClick={save}>
                保存
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
