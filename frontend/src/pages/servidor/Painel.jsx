import { useEffect, useMemo, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { AlertTriangle, CopyCheck, History, RefreshCw, Search, Sparkles } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { analiseApi, solicitacaoApi } from '../../lib/api'
import { AtualizarStatusSchema } from '../../lib/validation'
import { formatDate, formatDateTime } from '../../lib/format'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { Input, Select, Textarea } from '../../components/ui/Input'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'
import StatusBadge from '../../components/ui/StatusBadge'
import Modal from '../../components/ui/Modal'
import Badge from '../../components/ui/Badge'
import PhotoUpload from '../../components/forms/PhotoUpload'

const LIMIAR_IA = 0.6
const RAIO_DUPLICIDADE_M = 50

const STATUS_OPTS = [
  { value: 'EM_ANALISE',  label: 'Em análise'  },
  { value: 'AGENDADO',    label: 'Agendado'    },
  { value: 'EM_EXECUCAO', label: 'Em execução' },
  { value: 'CONCLUIDO',   label: 'Concluído'   },
  { value: 'CANCELADO',   label: 'Cancelado'   },
]

// Próximos status válidos por status atual (espelha a máquina de estados do back).
const TRANSICOES = {
  RECEBIDO:    ['EM_ANALISE', 'CANCELADO'],
  EM_ANALISE:  ['AGENDADO',   'CANCELADO'],
  AGENDADO:    ['EM_EXECUCAO','CANCELADO'],
  EM_EXECUCAO: ['CONCLUIDO',  'CANCELADO'],
}

export default function Painel() {
  // Fase 6: a ordem vem pronta do backend — fila de prioridade (PriorityQueue) por
  // dias restantes até o SLA. Antes o painel buscava /abertas e reordenava no navegador.
  const { data, loading, error, refresh } = useApi(solicitacaoApi.filaAtendimento, [])
  const items = data || []

  // Possíveis duplicados: agrupamento espacial (KD-Tree + Union-Find) das abertas
  const agrupamento = useApi(() => analiseApi.agrupamentos({
    raioMetros: RAIO_DUPLICIDADE_M, apenasAbertas: true, minimo: 2, limite: 200,
  }), [])

  const [filter, setFilter]   = useState('TODOS')
  const [search, setSearch]   = useState('')
  const [modalItem, setModal] = useState(null)
  const [auditoriaItem, setAuditoria] = useState(null)

  const posicaoNaFila = useMemo(() => {
    const m = new Map()
    items.forEach((i, idx) => m.set(i.id, idx + 1))
    return m
  }, [items])

  const grupoPorId = useMemo(() => {
    const m = new Map()
    for (const g of agrupamento.data?.grupos || []) {
      for (const membro of g.membros || []) m.set(membro.id, g)
    }
    return m
  }, [agrupamento.data])

  const filtered = useMemo(() => {
    let list = items
    if (filter !== 'TODOS') list = list.filter(i => i.status === filter)
    if (search) {
      const q = search.toLowerCase()
      list = list.filter(i =>
        i.protocolo?.toLowerCase().includes(q) ||
        i.tipoDescricao?.toLowerCase().includes(q) ||
        i.nomeBairro?.toLowerCase().includes(q) ||
        i.logradouro?.toLowerCase().includes(q)
      )
    }
    return list // filtrar não muda a ordem de prioridade definida pelo backend
  }, [items, filter, search])

  const counts = useMemo(() => ({
    RECEBIDO:    items.filter(i => i.status === 'RECEBIDO').length,
    ANDAMENTO:   items.filter(i => ['EM_ANALISE','AGENDADO','EM_EXECUCAO'].includes(i.status)).length,
    SLA_VENCIDO: items.filter(i => i.diasParaSla != null && i.diasParaSla < 0).length,
    DUPLICADOS:  items.filter(i => grupoPorId.has(i.id)).length,
  }), [items, grupoPorId])

  const atualizarTudo = () => { refresh(); agrupamento.refresh() }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="h-14 flex items-center justify-between px-5 border-b border-slate-200 bg-white flex-shrink-0">
        <div>
          <h1 className="text-sm font-bold text-slate-900">Painel de triagem</h1>
          <p className="text-[11px] text-slate-500">
            {filtered.length} de {items.length} exibidas · fila priorizada pela urgência do SLA
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={atualizarTudo}>
          <RefreshCw size={12} className={loading ? 'animate-spin' : ''}/> Atualizar
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-3 px-5 py-3 border-b border-slate-200 bg-white flex-shrink-0">
        {[
          { val: counts.RECEBIDO,    label: 'Aguardando triagem',  cls: 'text-red-600'   },
          { val: counts.ANDAMENTO,   label: 'Em andamento',        cls: 'text-amber-600' },
          { val: counts.SLA_VENCIDO, label: 'SLA vencido',         cls: 'text-red-600'   },
          { val: counts.DUPLICADOS,  label: 'Possíveis duplicados', cls: 'text-blue-600'  },
        ].map(s => (
          <Card key={s.label} className="px-4 py-2.5 text-center">
            <div className={`text-2xl font-black ${s.cls}`}>{s.val}</div>
            <div className="text-[9.5px] text-slate-500 uppercase tracking-wider mt-0.5">{s.label}</div>
          </Card>
        ))}
      </div>

      {/* Filtros */}
      <div className="flex gap-2 px-5 py-3 border-b border-slate-200 bg-white flex-shrink-0 overflow-x-auto">
        {['TODOS','RECEBIDO','EM_ANALISE','AGENDADO','EM_EXECUCAO'].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-semibold border transition-colors cursor-pointer
              ${filter === s
                ? 'bg-blue-50 border-blue-200 text-blue-700'
                : 'bg-white border-slate-200 text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
            {s === 'TODOS' ? 'Todos' : s.replace('_',' ').toLowerCase()}
          </button>
        ))}
        <div className="flex-1 min-w-[200px]">
          <Input placeholder="Buscar..." leftIcon={<Search size={13}/>}
            value={search} onChange={(e) => setSearch(e.target.value)}
            className="h-8"/>
        </div>
      </div>

      {/* Lista */}
      <div className="flex-1 overflow-y-auto">
        {loading ? (
          <div className="p-5 space-y-3">
            {[1,2,3,4].map(i => <Skeleton key={i} className="h-20"/>)}
          </div>
        ) : error ? (
          <ErrorState onRetry={refresh}/>
        ) : filtered.length === 0 ? (
          <EmptyState title="Tudo limpo!" description="Nenhuma solicitação no filtro atual."/>
        ) : (
          <ul className="divide-y divide-slate-100">
            {filtered.map(item => {
              const vencido = item.diasParaSla != null && item.diasParaSla < 0
              const grupo = grupoPorId.get(item.id)
              const iaDiverge = item.codigoTipoSugeridoIa && item.codigoTipoSugeridoIa !== item.codigoTipo
                && (item.confiancaIa ?? 0) >= LIMIAR_IA
              return (
                <li key={item.id} className={`px-5 py-3.5 hover:bg-slate-50 transition-colors
                                              ${vencido ? 'border-l-2 border-l-red-500' : ''}`}>
                  <div className="flex items-start gap-3">
                    <div className="w-8 flex-shrink-0 pt-0.5 text-center">
                      <div className="text-[9px] uppercase tracking-wider text-slate-400">fila</div>
                      <div className="text-sm font-black text-slate-700 font-mono">{posicaoNaFila.get(item.id)}</div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-[10px] text-slate-500">{item.protocolo}</span>
                        <PrazoSla dias={item.diasParaSla}/>
                      </div>
                      <div className="text-sm font-bold text-slate-900 mt-0.5 truncate">{item.tipoDescricao}</div>
                      <div className="text-[11px] text-slate-500 mt-0.5 truncate">
                        {item.logradouro && `${item.logradouro} · `}{item.nomeBairro}
                      </div>
                      <div className="text-[10px] text-slate-500 mt-1">
                        Aberto {formatDate(item.dataAbertura)}
                        {item.dataPrevisao && ` · Prazo ${formatDate(item.dataPrevisao)}`}
                      </div>
                      {(iaDiverge || grupo) && (
                        <div className="flex flex-wrap gap-1.5 mt-2">
                          {iaDiverge && (
                            <Badge variant="blue" className="normal-case tracking-normal">
                              <Sparkles size={10}/> IA sugere: {item.tipoSugeridoIa} ({Math.round(item.confiancaIa * 100)}%)
                            </Badge>
                          )}
                          {grupo && (
                            <Badge variant="amber" className="normal-case tracking-normal">
                              <CopyCheck size={10}/> Possível duplicado · {grupo.quantidade} chamados a até {RAIO_DUPLICIDADE_M} m
                            </Badge>
                          )}
                        </div>
                      )}
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <StatusBadge status={item.status}/>
                      <Button variant="ghost" size="sm" onClick={() => setAuditoria(item)} title="Trilha de auditoria">
                        <History size={13}/> Auditoria
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => setModal(item)}>
                        Atualizar
                      </Button>
                    </div>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>

      {/* Modais */}
      {modalItem && (
        <UpdateStatusModal item={modalItem} onClose={() => setModal(null)}
          onDone={() => { setModal(null); atualizarTudo() }}/>
      )}
      {auditoriaItem && (
        <AuditoriaModal item={auditoriaItem} onClose={() => setAuditoria(null)}/>
      )}
    </div>
  )
}

/** Situação do prazo calculada no backend (diasParaSla: negativo = vencido). */
function PrazoSla({ dias }) {
  if (dias == null) return null
  if (dias < 0) {
    return (
      <span className="flex items-center gap-1 text-[10px] text-red-600 font-semibold">
        <AlertTriangle size={10}/> SLA vencido há {Math.abs(dias)} {Math.abs(dias) === 1 ? 'dia' : 'dias'}
      </span>
    )
  }
  if (dias <= 2) {
    return (
      <span className="text-[10px] text-amber-600 font-semibold">
        {dias === 0 ? 'Vence hoje' : `Vence em ${dias} ${dias === 1 ? 'dia' : 'dias'}`}
      </span>
    )
  }
  return <span className="text-[10px] text-slate-500">Prazo em {dias} dias</span>
}

/**
 * Trilha de auditoria da solicitação, da transição mais recente para a mais antiga.
 * Consome GET /solicitacoes/{id}/historico/auditoria — montada no backend com uma
 * pilha (Deque como LIFO). Até a Fase 5 essa rota existia, mas nenhuma tela a usava.
 */
function AuditoriaModal({ item, onClose }) {
  const [eventos, setEventos] = useState(null)
  const [erro, setErro] = useState(null)
  const [tentativa, setTentativa] = useState(0)

  useEffect(() => {
    solicitacaoApi.historicoAuditoria(item.id)
      .then(r => setEventos(r?.data || []))
      .catch(e => setErro(e))
  }, [item.id, tentativa])

  return (
    <Modal open onClose={onClose} title="Trilha de auditoria" size="lg">
      <div className="mb-4">
        <div className="font-mono text-[10px] text-slate-500">{item.protocolo}</div>
        <div className="text-sm font-bold text-slate-900 mt-0.5">{item.tipoDescricao}</div>
        <p className="text-[11px] text-slate-500 mt-1">
          Mais recente primeiro (pilha / LIFO) — registros imutáveis de HISTORICO_STATUS.
        </p>
      </div>
      {erro ? (
        <ErrorState onRetry={() => { setErro(null); setEventos(null); setTentativa(t => t + 1) }}/>
      ) : eventos == null ? (
        <div className="space-y-2">{[1,2,3].map(i => <Skeleton key={i} className="h-14"/>)}</div>
      ) : eventos.length === 0 ? (
        <EmptyState title="Sem transições" description="A solicitação ainda não saiu de Recebido."/>
      ) : (
        <ol className="space-y-2">
          {eventos.map((ev, idx) => (
            <li key={idx} className={`rounded-xl border px-3.5 py-2.5 ${idx === 0 ? 'border-blue-200 bg-blue-50/60' : 'border-slate-200 bg-white'}`}>
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 text-xs">
                  <StatusBadge status={ev.statusAnterior}/>
                  <span className="text-slate-400">→</span>
                  <StatusBadge status={ev.statusNovo}/>
                </div>
                {idx === 0 && <span className="text-[9.5px] font-bold uppercase tracking-wider text-blue-700">topo da pilha</span>}
              </div>
              <div className="text-[11px] text-slate-500 mt-1.5">
                {formatDateTime(ev.dataHora)} · {ev.nomeServidor}
              </div>
              {ev.justificativa && (
                <div className="text-[11px] text-slate-700 mt-1">“{ev.justificativa}”</div>
              )}
            </li>
          ))}
        </ol>
      )}
    </Modal>
  )
}

function UpdateStatusModal({ item, onClose, onDone }) {
  const validos = TRANSICOES[item.status] || []

  const {
    register, handleSubmit, control, watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(AtualizarStatusSchema),
    defaultValues: { novoStatus: '', justificativa: '', caminhoFotoDepois: '' },
  })

  const novoStatus = watch('novoStatus')

  async function onSubmit(values) {
    try {
      await solicitacaoApi.atualizarStatus(item.id, {
        novoStatus:        values.novoStatus,
        justificativa:     values.justificativa || null,
        caminhoFotoDepois: values.caminhoFotoDepois || null,
      })
      toast.success('Status atualizado')
      onDone()
    } catch (err) {
      toast.error('Falha ao atualizar', { description: err.message })
    }
  }

  return (
    <Modal open onClose={onClose} title="Atualizar status">
      <div className="mb-4">
        <div className="font-mono text-[10px] text-slate-500">{item.protocolo}</div>
        <div className="text-sm font-bold text-slate-900 mt-0.5">{item.tipoDescricao}</div>
        <div className="text-[11px] text-slate-500 mt-0.5">
          {item.logradouro && `${item.logradouro} · `}{item.nomeBairro}
        </div>
        <div className="text-[11px] mt-2 text-slate-500">
          Status atual: <StatusBadge status={item.status} className="ml-1"/>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <Select label="Novo status" required
          error={errors.novoStatus?.message} {...register('novoStatus')}>
          <option value="">Selecione...</option>
          {STATUS_OPTS.filter(o => validos.includes(o.value))
            .map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </Select>

        <Textarea
          label={`Justificativa${novoStatus === 'CANCELADO' ? ' *' : ''}`}
          required={novoStatus === 'CANCELADO'}
          rows={3}
          maxLength={500}
          placeholder="Descreva a ação tomada..."
          error={errors.justificativa?.message}
          {...register('justificativa')}
        />

        {novoStatus === 'CONCLUIDO' && (
          <Controller
            control={control} name="caminhoFotoDepois"
            render={({ field }) => (
              <PhotoUpload
                label="Foto do depois"
                required
                value={field.value}
                onChange={field.onChange}
                error={errors.caminhoFotoDepois?.message}/>
            )}
          />
        )}

        <div className="flex gap-2.5 pt-2">
          <Button variant="secondary" type="button" onClick={onClose} className="flex-1">Cancelar</Button>
          <Button type="submit" loading={isSubmitting} className="flex-1">Confirmar</Button>
        </div>
      </form>
    </Modal>
  )
}
