let refreshPromise

const saveTokens = data => {
  localStorage.setItem('accessToken', data.accessToken)
  localStorage.setItem('refreshToken', data.refreshToken)
  // 兼容旧版本前端缓存，后续版本可移除。
  localStorage.setItem('token', data.accessToken)
}

const clearTokens = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('token')
}

const refreshAccessToken = async () => {
  if (!refreshPromise) {
    refreshPromise = fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') })
    }).then(async response => {
      if (!response.ok) throw new Error('登录状态已过期，请重新登录')
      const data = await response.json()
      saveTokens(data)
    }).finally(() => { refreshPromise = undefined })
  }
  return refreshPromise
}

const request = async (url, options = {}, retried = false) => {
  const token = localStorage.getItem('accessToken') || localStorage.getItem('token')
  const response = await fetch(url, { ...options, headers: { ...(options.headers || {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
  if (response.status === 401 && !retried && localStorage.getItem('refreshToken')) {
    try { await refreshAccessToken(); return request(url, options, true) } catch (error) { clearTokens(); throw error }
  }
  if (!response.ok) { const body = await response.json().catch(() => ({})); throw new Error(body.detail || body.message || '请求失败') }
  return response
}

export const login = async (username, password) => {
  const response = await fetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) })
  if (!response.ok) { const body = await response.json().catch(() => ({})); throw new Error(body.detail || body.message || '登录失败') }
  const data = await response.json()
  saveTokens(data)
  return data
}
export const clearAuth = clearTokens
export const listDocs = async () => (await request('/api/documents')).json()
export const upload = async file => { const form = new FormData(); form.append('file', file); return (await request('/api/documents', { method: 'POST', body: form })).json() }
export const remove = id => request(`/api/documents/${id}`, { method: 'DELETE' })
export const contentBlob = async id => (await request(`/api/documents/${id}/content`)).blob()
export const contentText = async id => (await request(`/api/documents/${id}/content`)).text()
export const previewPdfBlob = async id => (await request(`/api/documents/${id}/preview`)).blob()
