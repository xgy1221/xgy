const TOKEN_KEY = 'xgy_web_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 统一请求：走 Vite 代理 /api → Spring Boot
 * 响应形如 { code, message, data }
 */
export async function request(path, { method = 'GET', data, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const res = await fetch(path.startsWith('/api') ? path : `/api${path}`, {
    method,
    headers,
    body: data != null ? JSON.stringify(data) : undefined
  })

  let body = null
  try {
    body = await res.json()
  } catch {
    body = null
  }

  if (res.status === 401 || (body && body.code === 401)) {
    clearToken()
    localStorage.removeItem('xgy_web_user')
    if (!window.location.pathname.startsWith('/login')) {
      window.location.assign('/login')
    }
    throw new Error((body && body.message) || '请重新登录')
  }

  if (!res.ok) {
    throw new Error((body && body.message) || `请求失败 (${res.status})`)
  }

  if (body && typeof body.code === 'number' && body.code !== 0) {
    throw new Error(body.message || '业务错误')
  }

  return body ? body.data : null
}
