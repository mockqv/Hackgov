import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { solicitacaoApi } from '../services/api'
import { useApp } from '../context/AppContext'
import { Search, Star } from 'lucide-react'

const STATUS_COLOR = {
  RECEBIDO:    { dot: 'bg-red-500',    text: 'text-red-400',    label: 'Recebido'     },
  EM_ANALISE:  { dot: 'bg-amber-500',  text: 'text-amber-400',  label: 'Em análise'   },
  AGENDADO:    { dot: 'bg-amber-500',  text: 'text-amber-400',  label: 'Agendado'     },
  EM_EXECUCAO: { dot: 'bg-blue-500',   text: 'text-blue-400',   label: 'Em execução'  },
  CONCLUIDO:   { dot: 'bg-green-500',  text: 'text-green-400',  label: 'Concluído'    },
  CANCELADO:   { dot: 'bg-gray-500',   text: 'text-gray-400',   label: 'Cancelado'    },
}

const STEPS = ['RECEBIDO','EM_ANALISE','AGENDADO','EM_EXECUCAO','CONCLUIDO']

export default function Acompanhar() {
  const [params] = useSearchParams()
  const { showToast } = useApp()
  const [proto, setProto] = useState(params.get('proto') || '')
  const [data, setData] = useState(null)
  const [hist, setHist] = useState([])
  const [loading, setLoading] = useState(false)
  const [rating, setRating] = useState(0)
  const [ratingLoading, setRatingLoading] = useState(false)

  async function buscar(e) {
    e?.preventDefault()
    if (!proto.trim()) return
    setLoading(true)
    try {
      const [det, his] = await Promise.all([
        solicitacaoApi.porProtocolo(proto.trim()),
        solicitacaoApi.historico(
          (await solicitacaoApi.porProtocolo(proto.trim())).data.id
        ).catch(() => ({ data: [] })),
      ])
      setData(det.data)
      setHist(his.data || [])
    } catch (err) {
      showToast('error', err.message)
      setData(null)
    } finally { setLoading(false) }
  }

  useEffect(() => { if (params.get('proto')) buscar() }, [])

  async function avaliar() {
    if (!rating) return showToast('error', 'Selecione uma nota')
    setRatingLoading(true)
    try {
      await solicitacaoApi.avaliar(data.id, { nota: rating })
      showToast('success', 'Avaliação registrada! Obrigado.')
    } catch (err) {
      showToast('error', err.message)
    } finally { setRatingLoading(false) }
  }

  const stepIdx = data ? STEPS.indexOf(data.status) : -1

  return (
    <div className="max-w-2xl mx-auto p-6 space-y-6">
      <div>
        <h1 className="text-lg font-bold text-white">Acompanhar solicitação</h1>
        <p className="text-xs text-gray-500 mt-0.5">Digite o protocolo para ver o status em tempo real</p>
      </div>

      {/* Search */}
      <form onSubmit={buscar} className="flex gap-2">
        <input className="input flex-1 font-mono" placeholder="ZEL-2026-XXXXXXXX"
          value={proto} onChange={e => setProto(e.target.value.toUpperCase())} />
        <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2 px-4">
          <Search size={14} />
          {loading ? 'Buscando...' : 'Buscar'}
        </button>
      </form>

      {data && (
        <div className="space-y-4">
          {/* Info card */}
          <div className="card p-5 space-y-3">
            <div className="flex items-start justify-between">
              <div>
                <div className="font-mono text-xs text-gray-500">{data.protocolo}</div>
                <div className="text-base font-bold text-white mt-1">{data.tipoDescricao}</div>
                <div className="text-xs text-gray-400 mt-0.5">
                  {data.localizacao?.logradouro && `${data.localizacao.logradouro}, `}
                  {data.localizacao?.nomeBairro}
                </div>
              </div>
              <span className={`badge text-[11px] px-2.5 py-1 rounded-xl font-bold
                ${STATUS_COLOR[data.status]?.text} bg-gray-800 border border-gray-700`}>
                {STATUS_COLOR[data.status]?.label || data.status}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-2 border-t border-gray-800 text-xs">
              <div>
                <div className="text-gray-500">Aberto em</div>
                <div className="text-gray-200 font-medium mt-0.5">
                  {new Date(data.dataAbertura).toLocaleDateString('pt-BR')}
                </div>
              </div>
              {data.dataPrevisao && (
                <div>
                  <div className="text-gray-500">Previsão</div>
                  <div className="text-gray-200 font-medium mt-0.5">
                    {new Date(data.dataPrevisao).toLocaleDateString('pt-BR')}
                  </div>
                </div>
              )}
              {data.nomeServidor && (
                <div>
                  <div className="text-gray-500">Responsável</div>
                  <div className="text-gray-200 font-medium mt-0.5">{data.nomeServidor}</div>
                </div>
              )}
              <div>
                <div className="text-gray-500">SLA</div>
                <div className="text-gray-200 font-medium mt-0.5">{data.slaDias} dias úteis</div>
              </div>
            </div>
          </div>

          {/* Step progress */}
          <div className="card p-5">
            <h3 className="text-sm font-semibold text-gray-300 mb-4">Progresso</h3>
            <div className="flex items-center gap-0">
              {STEPS.map((step, i) => {
                const done = i <= stepIdx
                const current = i === stepIdx
                const labels = ['Recebido','Em análise','Agendado','Execução','Concluído']
                return (
                  <div key={step} className="flex-1 flex flex-col items-center">
                    <div className="flex items-center w-full">
                      {i > 0 && <div className={`flex-1 h-0.5 ${done ? 'bg-blue-600' : 'bg-gray-700'}`}/>}
                      <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold border-2 flex-shrink-0
                        ${current ? 'bg-blue-600 border-blue-400 text-white shadow-lg shadow-blue-600/40'
                          : done ? 'bg-blue-900 border-blue-600 text-blue-300'
                          : 'bg-gray-800 border-gray-700 text-gray-600'}`}>
                        {done && !current ? '✓' : i + 1}
                      </div>
                      {i < STEPS.length - 1 && <div className={`flex-1 h-0.5 ${i < stepIdx ? 'bg-blue-600' : 'bg-gray-700'}`}/>}
                    </div>
                    <div className={`text-[9px] mt-1.5 font-medium text-center leading-tight
                      ${current ? 'text-blue-400' : done ? 'text-gray-400' : 'text-gray-600'}`}>
                      {labels[i]}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Timeline */}
          {hist.length > 0 && (
            <div className="card p-5">
              <h3 className="text-sm font-semibold text-gray-300 mb-4">Histórico</h3>
              <div className="space-y-4">
                {hist.map((h, i) => (
                  <div key={i} className="flex gap-3">
                    <div className="flex flex-col items-center">
                      <div className={`w-3 h-3 rounded-full flex-shrink-0 mt-0.5
                        ${STATUS_COLOR[h.statusNovo]?.dot || 'bg-gray-500'}`}/>
                      {i < hist.length - 1 && <div className="w-0.5 flex-1 bg-gray-800 mt-1.5 min-h-4"/>}
                    </div>
                    <div className="pb-4 flex-1">
                      <div className={`text-xs font-semibold ${STATUS_COLOR[h.statusNovo]?.text || 'text-gray-300'}`}>
                        {STATUS_COLOR[h.statusNovo]?.label || h.statusNovo}
                      </div>
                      <div className="text-[10px] text-gray-500 mt-0.5">
                        {new Date(h.dataHora).toLocaleString('pt-BR')} • {h.nomeServidor}
                      </div>
                      {h.justificativa && (
                        <div className="text-xs text-gray-400 mt-1 bg-gray-800 rounded-lg px-3 py-2">
                          {h.justificativa}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Photos */}
          {(data.caminhoFotoAntes || data.caminhoFotoDepois) && (
            <div className="card p-5">
              <h3 className="text-sm font-semibold text-gray-300 mb-3">Fotos</h3>
              <div className="grid grid-cols-2 gap-3">
                {data.caminhoFotoAntes && (
                  <div className="bg-gray-800 rounded-xl p-3 text-center">
                    <div className="text-xs text-gray-500 mb-1">Antes</div>
                    <div className="text-gray-400 text-sm font-mono truncate">{data.caminhoFotoAntes}</div>
                  </div>
                )}
                {data.caminhoFotoDepois && (
                  <div className="bg-gray-800 rounded-xl p-3 text-center border border-green-700/30">
                    <div className="text-xs text-green-500 mb-1">Depois ✓</div>
                    <div className="text-gray-400 text-sm font-mono truncate">{data.caminhoFotoDepois}</div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Avaliação */}
          {data.status === 'CONCLUIDO' && (
            <div className="card p-5">
              <h3 className="text-sm font-semibold text-gray-300 mb-3">Avaliar o serviço</h3>
              <div className="flex gap-2 justify-center mb-4">
                {[1,2,3,4,5].map(n => (
                  <button key={n} onClick={() => setRating(n)}
                    className="transition-transform hover:scale-110 active:scale-95">
                    <Star size={28} className={n <= rating ? 'text-yellow-400 fill-yellow-400' : 'text-gray-600'} />
                  </button>
                ))}
              </div>
              <button onClick={avaliar} disabled={ratingLoading || !rating} className="btn-primary w-full">
                {ratingLoading ? 'Enviando...' : 'Enviar avaliação'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
