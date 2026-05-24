import { useState, useEffect } from 'react'
import { solicitacaoApi } from '../services/api'
import { useApp } from '../context/AppContext'
import { RefreshCw, AlertTriangle } from 'lucide-react'

const STATUS_OPTS = ['EM_ANALISE','AGENDADO','EM_EXECUCAO','CONCLUIDO','CANCELADO']
const STATUS_COLOR = {
  RECEBIDO:    'text-red-400 bg-red-950 border-red-700',
  EM_ANALISE:  'text-amber-400 bg-amber-950 border-amber-700',
  AGENDADO:    'text-amber-400 bg-amber-950 border-amber-700',
  EM_EXECUCAO: 'text-blue-400 bg-blue-950 border-blue-700',
  CONCLUIDO:   'text-green-400 bg-green-950 border-green-700',
  CANCELADO:   'text-gray-400 bg-gray-800 border-gray-700',
}
const STATUS_LABEL = {
  RECEBIDO:'Recebido', EM_ANALISE:'Em análise', AGENDADO:'Agendado',
  EM_EXECUCAO:'Em execução', CONCLUIDO:'Concluído', CANCELADO:'Cancelado',
}
const BORDER_URGENCY = {
  RECEBIDO:'border-l-4 border-l-red-500',
  EM_ANALISE:'border-l-4 border-l-amber-500',
  AGENDADO:'border-l-4 border-l-amber-400',
  EM_EXECUCAO:'border-l-4 border-l-blue-500',
  CONCLUIDO:'border-l-4 border-l-green-600',
  CANCELADO:'border-l-4 border-l-gray-700',
}

export default function Painel() {
  const { user, showToast } = useApp()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('TODOS')
  const [updating, setUpdating] = useState(null) // id being updated
  const [modal, setModal] = useState(null)  // { item }
  const [updateForm, setUpdateForm] = useState({ novoStatus: '', justificativa: '', caminhoFotoDepois: '' })

  async function load() {
    setLoading(true)
    try {
      const res = await solicitacaoApi.abertas()
      setItems(res.data || [])
    } catch { setItems([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const filtered = filter === 'TODOS'
    ? items
    : items.filter(i => i.tipoDescricao?.toLowerCase().includes(filter.toLowerCase()))

  async function handleUpdate(e) {
    e.preventDefault()
    if (!updateForm.novoStatus) return
    setUpdating(modal.item.id)
    try {
      await solicitacaoApi.atualizarStatus(modal.item.id, {
        servidorId: user?.id || 1,
        novoStatus: updateForm.novoStatus,
        justificativa: updateForm.justificativa,
        caminhoFotoDepois: updateForm.caminhoFotoDepois || undefined,
      })
      showToast('success', 'Status atualizado com sucesso')
      setModal(null)
      setUpdateForm({ novoStatus: '', justificativa: '', caminhoFotoDepois: '' })
      load()
    } catch (err) {
      showToast('error', err.message)
    } finally { setUpdating(null) }
  }

  const chips = ['TODOS', 'Buraco', 'Iluminação', 'Poda', 'Entulho']

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="h-16 flex items-center justify-between px-6 border-b border-gray-800 flex-shrink-0">
        <div>
          <h1 className="text-base font-bold text-white">Painel de triagem</h1>
          <p className="text-xs text-gray-500">{filtered.length} solicitações abertas</p>
        </div>
        <button onClick={load} className="btn-secondary flex items-center gap-2 text-sm py-2 px-3">
          <RefreshCw size={13} />
          Atualizar
        </button>
      </div>

      {/* Stats */}
      <div className="flex gap-3 px-6 py-3 border-b border-gray-800 flex-shrink-0">
        {[
          { label: 'Recebidas',     val: items.filter(i => i.status === 'RECEBIDO').length,    color: 'red'   },
          { label: 'Em andamento',  val: items.filter(i => ['EM_ANALISE','AGENDADO','EM_EXECUCAO'].includes(i.status)).length, color: 'amber' },
          { label: 'Abertas total', val: items.length,                                          color: 'blue'  },
        ].map(({ label, val, color }) => (
          <div key={label} className="card px-4 py-2.5 flex-1 text-center">
            <div className={`text-2xl font-black text-${color}-400`}>{val}</div>
            <div className="text-[10px] text-gray-500 mt-0.5">{label}</div>
          </div>
        ))}
      </div>

      {/* Filter chips */}
      <div className="flex gap-2 px-6 py-3 border-b border-gray-800 flex-shrink-0 overflow-x-auto">
        {chips.map(c => (
          <button key={c} onClick={() => setFilter(c)}
            className={`flex-shrink-0 px-3.5 py-1.5 rounded-full text-xs font-semibold border transition-all
              ${filter === c
                ? 'bg-blue-600/20 border-blue-500 text-blue-400'
                : 'bg-gray-900 border-gray-700 text-gray-400 hover:text-gray-200'}`}>
            {c}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="flex-1 overflow-y-auto divide-y divide-gray-800/50">
        {loading ? (
          <div className="p-8 text-center text-gray-500">Carregando...</div>
        ) : filtered.length === 0 ? (
          <div className="p-8 text-center text-gray-600 text-sm">Nenhuma solicitação aberta</div>
        ) : (
          filtered.map(item => (
            <div key={item.id}
              className={`px-6 py-4 hover:bg-gray-900/40 transition-colors ${BORDER_URGENCY[item.status] || ''}`}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-mono text-[10px] text-gray-500">{item.protocolo}</span>
                    {item.status === 'RECEBIDO' && (
                      <span className="flex items-center gap-1 text-[10px] text-red-400 font-semibold">
                        <AlertTriangle size={10} /> Aguardando triagem
                      </span>
                    )}
                  </div>
                  <div className="text-sm font-bold text-white mt-0.5">{item.tipoDescricao}</div>
                  <div className="text-xs text-gray-400 mt-0.5">
                    {item.logradouro && `${item.logradouro} · `}{item.nomeBairro}
                  </div>
                  <div className="text-[10px] text-gray-600 mt-1">
                    Aberto em {new Date(item.dataAbertura).toLocaleDateString('pt-BR')}
                    {item.dataPrevisao && ` · Prazo: ${new Date(item.dataPrevisao).toLocaleDateString('pt-BR')}`}
                  </div>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <span className={`badge text-[10px] px-2.5 py-1 rounded-xl border ${STATUS_COLOR[item.status]}`}>
                    {STATUS_LABEL[item.status]}
                  </span>
                  <button onClick={() => { setModal({ item }); setUpdateForm({ novoStatus: '', justificativa: '', caminhoFotoDepois: '' }) }}
                    className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-gray-800 border border-gray-700
                               text-blue-400 hover:bg-blue-600/10 hover:border-blue-500/40 transition-all">
                    Atualizar
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Update Modal */}
      {modal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={e => e.target === e.currentTarget && setModal(null)}>
          <div className="card p-6 w-full max-w-md space-y-4">
            <div className="flex items-start justify-between">
              <div>
                <div className="font-mono text-xs text-gray-500">{modal.item.protocolo}</div>
                <div className="text-sm font-bold text-white mt-1">{modal.item.tipoDescricao}</div>
              </div>
              <button onClick={() => setModal(null)} className="text-gray-500 hover:text-gray-200 text-xl leading-none">×</button>
            </div>

            <form onSubmit={handleUpdate} className="space-y-4">
              <div>
                <label className="label">Novo status *</label>
                <select className="input" value={updateForm.novoStatus}
                  onChange={e => setUpdateForm(p => ({ ...p, novoStatus: e.target.value }))}>
                  <option value="">Selecione...</option>
                  {STATUS_OPTS.map(s => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Justificativa</label>
                <textarea className="input resize-none" rows={3}
                  placeholder="Descreva a ação tomada..."
                  value={updateForm.justificativa}
                  onChange={e => setUpdateForm(p => ({ ...p, justificativa: e.target.value }))} />
              </div>
              {updateForm.novoStatus === 'CONCLUIDO' && (
                <div>
                  <label className="label">Foto do depois (caminho/URL)</label>
                  <input className="input" placeholder="/uploads/depois.jpg"
                    value={updateForm.caminhoFotoDepois}
                    onChange={e => setUpdateForm(p => ({ ...p, caminhoFotoDepois: e.target.value }))} />
                </div>
              )}
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(null)} className="btn-secondary flex-1">Cancelar</button>
                <button type="submit" disabled={!!updating} className="btn-primary flex-1">
                  {updating ? 'Salvando...' : 'Confirmar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
