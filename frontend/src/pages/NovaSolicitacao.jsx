import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext'
import { solicitacaoApi, tipoServicoApi } from '../services/api'
import { MapPin, Upload, CheckCircle, ArrowLeft } from 'lucide-react'

export default function NovaSolicitacao() {
  const { user, showToast } = useApp()
  const navigate = useNavigate()
  const [tipos, setTipos] = useState([])
  const [loading, setLoading] = useState(false)
  const [protocolo, setProtocolo] = useState(null)

  const [form, setForm] = useState({
    cidadaoId: user?.id || 1,
    tipoServicoId: '',
    descricao: '',
    latitude: -23.5505,
    longitude: -46.6333,
    logradouro: '',
    numero: '',
    complemento: '',
    cep: '',
    bairroId: 1,
    caminhoFotoAntes: '',
  })

  useEffect(() => {
    tipoServicoApi.listar().then(r => setTipos(r.data || [])).catch(() => {})
  }, [])

  const set = (k, v) => setForm(p => ({ ...p, [k]: v }))

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.tipoServicoId) return showToast('error', 'Selecione o tipo de serviço')
    if (form.descricao.length < 10) return showToast('error', 'Descrição muito curta')
    setLoading(true)
    try {
      const res = await solicitacaoApi.criar({ ...form, tipoServicoId: Number(form.tipoServicoId) })
      setProtocolo(res.data.protocolo)
    } catch (err) {
      showToast('error', err.message)
    } finally {
      setLoading(false)
    }
  }

  // Success screen
  if (protocolo) {
    return (
      <div className="flex items-center justify-center min-h-full p-8">
        <div className="card p-8 max-w-md w-full text-center space-y-6">
          <div className="w-16 h-16 rounded-full bg-green-950 border border-green-700 flex items-center justify-center mx-auto">
            <CheckCircle size={32} className="text-green-400" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Solicitação enviada!</h2>
            <p className="text-gray-400 text-sm mt-2">Seu chamado foi registrado e já está no mapa.</p>
          </div>
          <div className="bg-gray-800 rounded-xl p-4 space-y-2">
            <div className="text-xs text-gray-500 uppercase tracking-wider">Número de protocolo</div>
            <div className="font-mono text-xl font-bold text-blue-400 tracking-widest">{protocolo}</div>
            <button onClick={() => navigator.clipboard?.writeText(protocolo).then(() => showToast('success', 'Copiado!'))}
              className="text-xs text-blue-400 hover:text-blue-300 border border-blue-500/30 rounded-lg px-3 py-1.5 transition-colors">
              Copiar protocolo
            </button>
          </div>
          <div className="flex gap-3">
            <button onClick={() => navigate('/acompanhar?proto=' + protocolo)} className="btn-primary flex-1">
              Acompanhar
            </button>
            <button onClick={() => { setProtocolo(null); setForm(p => ({ ...p, descricao: '', logradouro: '' })) }}
              className="btn-secondary flex-1">
              Nova solicitação
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)} className="p-2 rounded-xl hover:bg-gray-800 transition-colors">
          <ArrowLeft size={18} className="text-gray-400" />
        </button>
        <div>
          <h1 className="text-lg font-bold text-white">Nova Solicitação</h1>
          <p className="text-xs text-gray-500">Registre um problema urbano para que a prefeitura possa resolver</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">

        {/* Tipo */}
        <div className="card p-5">
          <h2 className="text-sm font-semibold text-gray-300 mb-4">Tipo de serviço</h2>
          <div>
            <label className="label">Categoria *</label>
            <select className="input" value={form.tipoServicoId} onChange={e => set('tipoServicoId', e.target.value)}>
              <option value="">Selecione...</option>
              {tipos.length > 0
                ? tipos.map(t => <option key={t.id} value={t.id}>{t.descricao} — SLA {t.slaDias} dias</option>)
                : <>
                    <option value="1">Buraco na via — SLA 7 dias</option>
                    <option value="2">Iluminação queimada — SLA 5 dias</option>
                    <option value="3">Poda de árvore — SLA 14 dias</option>
                    <option value="4">Remoção de entulho — SLA 7 dias</option>
                    <option value="5">Outros — SLA 10 dias</option>
                  </>
              }
            </select>
          </div>
        </div>

        {/* Descrição */}
        <div className="card p-5">
          <h2 className="text-sm font-semibold text-gray-300 mb-4">Descrição do problema</h2>
          <div>
            <label className="label">Descreva o problema * (mín. 10 caracteres)</label>
            <textarea className="input resize-none" rows={4}
              placeholder="Ex: Buraco grande na esquina próxima à padaria, dificultando a passagem de veículos..."
              value={form.descricao}
              onChange={e => set('descricao', e.target.value)}
              maxLength={500} />
            <div className="text-right text-xs text-gray-600 mt-1">{form.descricao.length}/500</div>
          </div>
        </div>

        {/* Localização */}
        <div className="card p-5">
          <h2 className="text-sm font-semibold text-gray-300 mb-4 flex items-center gap-2">
            <MapPin size={14} className="text-blue-400" /> Localização
          </h2>
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className="label">Logradouro</label>
              <input className="input" placeholder="Rua das Flores"
                value={form.logradouro} onChange={e => set('logradouro', e.target.value)} />
            </div>
            <div>
              <label className="label">Número</label>
              <input className="input" placeholder="100"
                value={form.numero} onChange={e => set('numero', e.target.value)} />
            </div>
            <div>
              <label className="label">CEP</label>
              <input className="input" placeholder="01310100" maxLength={8}
                value={form.cep} onChange={e => set('cep', e.target.value)} />
            </div>
            <div>
              <label className="label">Complemento</label>
              <input className="input" placeholder="Apto, Bloco..."
                value={form.complemento} onChange={e => set('complemento', e.target.value)} />
            </div>
            <div>
              <label className="label">Bairro ID *</label>
              <input className="input" type="number" placeholder="1"
                value={form.bairroId} onChange={e => set('bairroId', Number(e.target.value))} />
            </div>
            <div>
              <label className="label">Latitude *</label>
              <input className="input" type="number" step="any"
                value={form.latitude} onChange={e => set('latitude', parseFloat(e.target.value))} />
            </div>
            <div>
              <label className="label">Longitude *</label>
              <input className="input" type="number" step="any"
                value={form.longitude} onChange={e => set('longitude', parseFloat(e.target.value))} />
            </div>
          </div>
        </div>

        {/* Foto */}
        <div className="card p-5">
          <h2 className="text-sm font-semibold text-gray-300 mb-4 flex items-center gap-2">
            <Upload size={14} className="text-blue-400" /> Foto do problema
          </h2>
          <div>
            <label className="label">Caminho / URL da foto (opcional)</label>
            <input className="input" placeholder="/uploads/foto.jpg ou https://..."
              value={form.caminhoFotoAntes} onChange={e => set('caminhoFotoAntes', e.target.value)} />
          </div>
        </div>

        <button type="submit" disabled={loading} className="btn-primary w-full py-3 text-base">
          {loading ? 'Enviando...' : 'Enviar solicitação →'}
        </button>
      </form>
    </div>
  )
}
