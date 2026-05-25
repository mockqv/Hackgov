import axios from 'axios'

export const TOKEN_KEY = 'hackgov:token'

// ── eventos ─────────────────────────────────────────────────
// 401 emite "auth:logout" para o AuthContext limpar estado.
const bus = new EventTarget()
export const onAuthLogout = (cb) => {
  const handler = (e) => cb(e.detail)
  bus.addEventListener('auth:logout', handler)
  return () => bus.removeEventListener('auth:logout', handler)
}

// ── instância axios ─────────────────────────────────────────
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// Request: anexa Bearer se houver token
api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// Response: desempacota ApiResponse<T>; trata 401/403
api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const payload = err.response?.data
    const msg = payload?.message || err.message || 'Erro desconhecido'

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      bus.dispatchEvent(new CustomEvent('auth:logout', { detail: { reason: 'unauthorized' } }))
    }

    const error = new Error(msg)
    error.status = status
    error.payload = payload
    return Promise.reject(error)
  }
)

// ── endpoints ───────────────────────────────────────────────
export const authApi = {
  login:    (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  me:       ()     => api.get('/auth/me'),
}

export const solicitacaoApi = {
  criar:           (data)     => api.post('/solicitacoes', data),
  porProtocolo:    (proto)    => api.get(`/solicitacoes/protocolo/${encodeURIComponent(proto)}`),
  porId:           (id)       => api.get(`/solicitacoes/${id}`),
  minhas:          ()         => api.get('/solicitacoes/minhas'),
  mapa:            ()         => api.get('/solicitacoes/mapa'),
  abertas:         ()         => api.get('/solicitacoes/abertas'),
  atualizarStatus: (id, data) => api.patch(`/solicitacoes/${id}/status`, data),
  historico:       (id)       => api.get(`/solicitacoes/${id}/historico`),
  avaliar:         (id, data) => api.post(`/solicitacoes/${id}/avaliar`, data),
}

export const tipoServicoApi = {
  listar: () => api.get('/tipos-servico'),
}

export const dashboardApi = {
  obter: () => api.get('/dashboard'),
}

// ── ViaCEP (externo, não passa pelo interceptor) ────────────
export const cepLookup = async (cep) => {
  const clean = cep.replace(/\D/g, '')
  if (clean.length !== 8) return null
  try {
    const r = await fetch(`https://viacep.com.br/ws/${clean}/json/`)
    const data = await r.json()
    if (data.erro) return null
    return {
      logradouro: data.logradouro || '',
      bairro:     data.bairro     || '',
      cidade:     data.localidade || '',
      uf:         data.uf         || '',
    }
  } catch { return null }
}

// ── Nominatim (OpenStreetMap) — geocoding gratuito ──────────
// TOS: máx 1 req/s. Usado em ação de usuário (click/drag/digitação completa de CEP).
const NOMINATIM = 'https://nominatim.openstreetmap.org'

/** lat/lng → endereço */
export const nominatimReverse = async ({ lat, lng }) => {
  try {
    const url = `${NOMINATIM}/reverse?format=jsonv2&lat=${lat}&lon=${lng}&accept-language=pt-BR&zoom=18&addressdetails=1`
    const r = await fetch(url, { headers: { Accept: 'application/json' } })
    if (!r.ok) return null
    const data = await r.json()
    const a = data.address || {}
    return {
      logradouro: a.road || a.pedestrian || a.path || a.footway || '',
      bairro:     a.suburb || a.neighbourhood || a.quarter || a.city_district || '',
      cidade:     a.city || a.town || a.village || a.municipality || '',
      uf:         (a['ISO3166-2-lvl4'] || '').split('-')[1] || '',
      cep:        (a.postcode || '').replace(/\D/g, ''),
      display:    data.display_name || '',
    }
  } catch { return null }
}

/** endereço → lat/lng. Tenta CEP primeiro, depois rua/cidade. */
export const nominatimForward = async ({ cep, logradouro, numero, cidade, uf }) => {
  const tryQuery = async (qs) => {
    try {
      const r = await fetch(`${NOMINATIM}/search?${qs}&format=jsonv2&countrycodes=br&limit=1&addressdetails=0`, {
        headers: { Accept: 'application/json' },
      })
      if (!r.ok) return null
      const arr = await r.json()
      if (!Array.isArray(arr) || arr.length === 0) return null
      return { lat: parseFloat(arr[0].lat), lng: parseFloat(arr[0].lon), display: arr[0].display_name }
    } catch { return null }
  }

  // 1) CEP é o mais preciso
  if (cep && cep.replace(/\D/g, '').length === 8) {
    const result = await tryQuery(`postalcode=${cep.replace(/\D/g, '')}`)
    if (result) return result
  }
  // 2) Rua + cidade + UF
  if (logradouro && cidade) {
    const street = numero ? `${numero} ${logradouro}` : logradouro
    const qs = new URLSearchParams({
      street, city: cidade, state: uf || '', country: 'Brasil',
    }).toString()
    return tryQuery(qs)
  }
  return null
}

export default api
