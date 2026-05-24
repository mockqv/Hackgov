import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

// Response interceptor — unwrap ApiResponse<T>
api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err.response?.data?.message || err.message || 'Erro desconhecido'
    return Promise.reject(new Error(msg))
  }
)

// ── SOLICITAÇÕES ────────────────────────────────────────────
export const solicitacaoApi = {
  criar:          (data)       => api.post('/solicitacoes', data),
  porProtocolo:   (proto)      => api.get(`/solicitacoes/protocolo/${proto}`),
  porId:          (id)         => api.get(`/solicitacoes/${id}`),
  porCidadao:     (cidId)      => api.get(`/solicitacoes/cidadao/${cidId}`),
  mapa:           ()           => api.get('/solicitacoes/mapa'),
  abertas:        ()           => api.get('/solicitacoes/abertas'),
  atualizarStatus:(id, data)   => api.patch(`/solicitacoes/${id}/status`, data),
  historico:      (id)         => api.get(`/solicitacoes/${id}/historico`),
  avaliar:        (id, data)   => api.post(`/solicitacoes/${id}/avaliar`, data),
}

// ── CIDADÃOS ────────────────────────────────────────────────
export const cidadaoApi = {
  cadastrar: (data) => api.post('/cidadaos', data),
  buscar:    (id)   => api.get(`/cidadaos/${id}`),
}

// ── TIPOS DE SERVIÇO ────────────────────────────────────────
export const tipoServicoApi = {
  listar: () => api.get('/tipos-servico'),
}

// ── DASHBOARD ────────────────────────────────────────────────
export const dashboardApi = {
  obter: () => api.get('/dashboard'),
}

export default api
