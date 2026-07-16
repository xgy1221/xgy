import { useEffect, useState } from 'react'
import { canEdit } from '../auth/roles'
import { listActivities, saveActivity } from '../data/store'

const empty = {
  title: '',
  category: '比赛',
  startDate: '',
  endDate: '',
  address: '',
  summary: '',
  highlights: '',
  published: true
}

export default function Activities() {
  const editable = canEdit('activities')
  const [tab, setTab] = useState('open')
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(empty)

  async function reload(nextTab = tab) {
    setLoading(true)
    setError('')
    try {
      setList(await listActivities(nextTab))
    } catch (e) {
      setError(e.message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    reload(tab)
  }, [tab])

  async function save() {
    if (!form.title.trim()) return alert('请填写标题')
    try {
      await saveActivity(form)
      setOpen(false)
      setForm(empty)
      await reload()
    } catch (e) {
      alert(e.message || '保存失败')
    }
  }

  return (
    <div className="panel">
      <div className="toolbar">
        <div>
          <strong>比赛活动</strong>
          <div className="muted">家长可在小程序报名；此处由教务维护开放/往期活动</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className={`btn ${tab === 'open' ? '' : 'ghost'}`} type="button" onClick={() => setTab('open')}>
            进行中
          </button>
          <button className={`btn ${tab === 'past' ? '' : 'ghost'}`} type="button" onClick={() => setTab('past')}>
            往期
          </button>
          {editable && (
            <button
              className="btn"
              type="button"
              onClick={() => {
                setForm(empty)
                setOpen(true)
              }}
            >
              发布活动
            </button>
          )}
        </div>
      </div>
      {error && <p className="muted" style={{ color: '#b45309' }}>{error}</p>}
      {loading ? (
        <p className="muted">加载中…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>标题</th>
              <th>时间</th>
              <th>地点</th>
              <th>报名</th>
            </tr>
          </thead>
          <tbody>
            {list.map((a) => (
              <tr key={a.id}>
                <td>
                  <strong>{a.title}</strong>
                  <div className="muted">{a.category || a.summary || ''}</div>
                </td>
                <td>
                  {a.startDate || '-'}
                  {a.endDate ? ` ~ ${a.endDate}` : ''}
                </td>
                <td>{a.address || '-'}</td>
                <td>{a.signupCount != null ? a.signupCount : a.enrolledCount != null ? a.enrolledCount : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {open && (
        <div className="modal-mask">
          <div className="modal">
            <h3>发布活动</h3>
            <div className="form-grid">
              <div className="field full">
                <label>标题</label>
                <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
              </div>
              <div className="field">
                <label>开始日</label>
                <input
                  type="date"
                  value={form.startDate}
                  onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                />
              </div>
              <div className="field">
                <label>结束日</label>
                <input
                  type="date"
                  value={form.endDate}
                  onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                />
              </div>
              <div className="field full">
                <label>地点</label>
                <input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </div>
              <div className="field full">
                <label>简介</label>
                <textarea rows={2} value={form.summary} onChange={(e) => setForm({ ...form, summary: e.target.value })} />
              </div>
              <div className="field full">
                <label>亮点（逗号分隔）</label>
                <input value={form.highlights} onChange={(e) => setForm({ ...form, highlights: e.target.value })} />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn ghost" type="button" onClick={() => setOpen(false)}>
                取消
              </button>
              <button className="btn" type="button" onClick={save}>
                发布
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
