import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext'
import { cidadaoApi } from '../services/api'

export default function Login() {
  const { login, showToast } = useApp()
  const navigate = useNavigate()
  const [tab, setTab] = useState('login')   // 'login' | 'cadastro'
  const [loading, setLoading] = useState(false)

  // Login form
  const [loginForm, setLoginForm] = useState({ id: '', role: 'cidadao' })

  // Cadastro form
  const [cadForm, setCadForm] = useState({ nome: '', email: '', cpf: '', telefone: '' })

  async function handleLogin(e) {
    e.preventDefault()
    if (!loginForm.id) return showToast('error', 'Informe o ID do usuário')
    login({ id: Number(loginForm.id), nome: `Usuário #${loginForm.id}`, role: loginForm.role })
    navigate(loginForm.role === 'servidor' ? '/painel' : '/mapa')
  }

  async function handleCadastro(e) {
    e.preventDefault()
    setLoading(true)
    try {
      const res = await cidadaoApi.cadastrar(cadForm)
      showToast('success', `Cadastro realizado! Seu ID: ${res.data.id}`)
      login({ id: res.data.id, nome: res.data.nome, role: 'cidadao' })
      navigate('/mapa')
    } catch (err) {
      showToast('error', err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950 px-4">
      <div className="w-full max-w-md">

        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-blue-600 text-white font-black text-2xl mb-4 shadow-lg shadow-blue-600/30">
            H
          </div>
          <h1 className="text-2xl font-bold text-white">HackGov</h1>
          <p className="text-gray-400 text-sm mt-1">Sistema de Zeladoria Urbana</p>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 p-1 bg-gray-900 rounded-2xl mb-6 border border-gray-800">
          {['login', 'cadastro'].map(t => (
            <button key={t} onClick={() => setTab(t)}
              className={`flex-1 py-2.5 rounded-xl text-sm font-semibold transition-all duration-150
                ${tab === t ? 'bg-blue-600 text-white shadow' : 'text-gray-400 hover:text-gray-200'}`}>
              {t === 'login' ? 'Entrar' : 'Cadastrar'}
            </button>
          ))}
        </div>

        <div className="card p-6">

          {/* LOGIN */}
          {tab === 'login' && (
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="label">ID de usuário</label>
                <input className="input" type="number" placeholder="Ex: 1"
                  value={loginForm.id}
                  onChange={e => setLoginForm(p => ({ ...p, id: e.target.value }))} />
              </div>
              <div>
                <label className="label">Perfil</label>
                <select className="input" value={loginForm.role}
                  onChange={e => setLoginForm(p => ({ ...p, role: e.target.value }))}>
                  <option value="cidadao">Cidadão</option>
                  <option value="servidor">Servidor Público</option>
                </select>
              </div>
              <button type="submit" className="btn-primary w-full mt-2">Entrar</button>
              <p className="text-center text-xs text-gray-500 mt-2">
                Para demonstração, use ID 1 (cidadão) ou ID 1 (servidor)
              </p>
            </form>
          )}

          {/* CADASTRO */}
          {tab === 'cadastro' && (
            <form onSubmit={handleCadastro} className="space-y-4">
              <div>
                <label className="label">Nome completo *</label>
                <input className="input" placeholder="João Silva"
                  value={cadForm.nome}
                  onChange={e => setCadForm(p => ({ ...p, nome: e.target.value }))} />
              </div>
              <div>
                <label className="label">E-mail *</label>
                <input className="input" type="email" placeholder="joao@email.com"
                  value={cadForm.email}
                  onChange={e => setCadForm(p => ({ ...p, email: e.target.value }))} />
              </div>
              <div>
                <label className="label">CPF * (somente números)</label>
                <input className="input" placeholder="12345678901" maxLength={11}
                  value={cadForm.cpf}
                  onChange={e => setCadForm(p => ({ ...p, cpf: e.target.value }))} />
              </div>
              <div>
                <label className="label">Telefone</label>
                <input className="input" placeholder="11999990000"
                  value={cadForm.telefone}
                  onChange={e => setCadForm(p => ({ ...p, telefone: e.target.value }))} />
              </div>
              <button type="submit" disabled={loading} className="btn-primary w-full mt-2">
                {loading ? 'Cadastrando...' : 'Criar conta'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
