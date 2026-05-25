import { useMemo, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { AlertTriangle, RefreshCw, Search } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { solicitacaoApi } from '../../lib/api'
import { AtualizarStatusSchema } from '../../lib/validation'
import { daysBetween, formatDate } from '../../lib/format'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { Input, Select, Textarea } from '../../components/ui/Input'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'
import StatusBadge from '../../components/ui/StatusBadge'
import Modal from '../../components/ui/Modal'
import PhotoUpload from '../../components/forms/PhotoUpload'

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
  const { data, loading, error, refresh } = useApi(solicitacaoApi.abertas, [])
  const items = data || []

  const [filter, setFilter]   = useState('TODOS')
  const [search, setSearch]   = useState('')
  const [modalItem, setModal] = useState(null)

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
    // ordena por urgência: SLA estourado primeiro, depois mais antigos
    return [...list].sort((a, b) => {
      const da = daysBetween(a.dataAbertura) - (a.dataPrevisao ? daysBetween(a.dataPrevisao) : 0)
      const db = daysBetween(b.dataAbertura) - (b.dataPrevisao ? daysBetween(b.dataPrevisao) : 0)
      return db - da
    })
  }, [items, filter, search])

  const counts = useMemo(() => ({
    RECEBIDO:    items.filter(i => i.status === 'RECEBIDO').length,
    ANDAMENTO:   items.filter(i => ['EM_ANALISE','AGENDADO','EM_EXECUCAO'].includes(i.status)).length,
    SLA_VENCIDO: items.filter(i => i.dataPrevisao && new Date(i.dataPrevisao) < new Date()).length,
    TOTAL:       items.length,
  }), [items])

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="h-14 flex items-center justify-between px-5 border-b border-slate-800 flex-shrink-0">
        <div>
          <h1 className="text-sm font-bold text-white">Painel de triagem</h1>
          <p className="text-[11px] text-slate-500">{filtered.length} de {items.length} exibidas</p>
        </div>
        <Button variant="secondary" size="sm" onClick={refresh}>
          <RefreshCw size={12} className={loading ? 'animate-spin' : ''}/> Atualizar
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-3 px-5 py-3 border-b border-slate-800 flex-shrink-0">
        {[
          { val: counts.RECEBIDO,    label: 'Aguardando triagem', cls: 'text-red-400'   },
          { val: counts.ANDAMENTO,   label: 'Em andamento',       cls: 'text-amber-400' },
          { val: counts.SLA_VENCIDO, label: 'SLA vencido',        cls: 'text-red-400'   },
          { val: counts.TOTAL,       label: 'Total aberto',       cls: 'text-blue-400'  },
        ].map(s => (
          <Card key={s.label} className="px-4 py-2.5 text-center">
            <div className={`text-2xl font-black ${s.cls}`}>{s.val}</div>
            <div className="text-[9.5px] text-slate-500 uppercase tracking-wider mt-0.5">{s.label}</div>
          </Card>
        ))}
      </div>

      {/* Filtros */}
      <div className="flex gap-2 px-5 py-3 border-b border-slate-800 flex-shrink-0 overflow-x-auto">
        {['TODOS','RECEBIDO','EM_ANALISE','AGENDADO','EM_EXECUCAO'].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-semibold border transition-colors
              ${filter === s
                ? 'bg-blue-600/20 border-blue-500/40 text-blue-300'
                : 'bg-slate-900 border-slate-700/60 text-slate-400 hover:text-slate-200'}`}>
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
          <ul className="divide-y divide-slate-800/60">
            {filtered.map(item => {
              const vencido = item.dataPrevisao && new Date(item.dataPrevisao) < new Date()
              return (
                <li key={item.id} className={`px-5 py-3.5 hover:bg-slate-900/40 transition-colors
                                              ${vencido ? 'border-l-2 border-l-red-500' : ''}`}>
                  <div className="flex items-start gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-[10px] text-slate-500">{item.protocolo}</span>
                        {vencido && (
                          <span className="flex items-center gap-1 text-[10px] text-red-400 font-semibold">
                            <AlertTriangle size={10}/> SLA vencido
                          </span>
                        )}
                      </div>
                      <div className="text-sm font-bold text-white mt-0.5 truncate">{item.tipoDescricao}</div>
                      <div className="text-[11px] text-slate-400 mt-0.5 truncate">
                        {item.logradouro && `${item.logradouro} · `}{item.nomeBairro}
                      </div>
                      <div className="text-[10px] text-slate-500 mt-1">
                        Aberto {formatDate(item.dataAbertura)}
                        {item.dataPrevisao && ` · Prazo ${formatDate(item.dataPrevisao)}`}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <StatusBadge status={item.status}/>
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

      {/* Modal */}
      {modalItem && (
        <UpdateStatusModal item={modalItem} onClose={() => setModal(null)} onDone={() => { setModal(null); refresh() }}/>
      )}
    </div>
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
        <div className="text-sm font-bold text-white mt-0.5">{item.tipoDescricao}</div>
        <div className="text-[11px] text-slate-400 mt-0.5">
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
