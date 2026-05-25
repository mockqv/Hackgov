import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { Search, Star } from 'lucide-react'

import { solicitacaoApi } from '../../lib/api'
import { AvaliacaoSchema } from '../../lib/validation'
import { formatDate, formatDateTime } from '../../lib/format'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { Input, Textarea } from '../../components/ui/Input'
import Skeleton from '../../components/ui/Skeleton'
import StatusBadge from '../../components/ui/StatusBadge'

const STEPS = ['RECEBIDO', 'EM_ANALISE', 'AGENDADO', 'EM_EXECUCAO', 'CONCLUIDO']
const LABELS = ['Recebido', 'Em análise', 'Agendado', 'Execução', 'Concluído']

export default function Acompanhar() {
  const { protocolo: routeProto } = useParams()
  const navigate = useNavigate()

  const [proto, setProto] = useState(routeProto || '')
  const [data, setData]   = useState(null)
  const [hist, setHist]   = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)

  async function buscar(p = proto) {
    const target = (p || '').trim().toUpperCase()
    if (!target) return
    setLoading(true)
    setSearched(true)
    try {
      const det = await solicitacaoApi.porProtocolo(target)
      setData(det.data)
      try {
        const his = await solicitacaoApi.historico(det.data.id)
        setHist(his.data || [])
      } catch {
        setHist([])
      }
      navigate(`/acompanhar/${target}`, { replace: true })
    } catch (err) {
      setData(null); setHist([])
      toast.error('Não encontrado', { description: err.message })
    } finally { setLoading(false) }
  }

  useEffect(() => {
    if (routeProto) buscar(routeProto)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeProto])

  const stepIdx = data ? STEPS.indexOf(data.status) : -1

  return (
    <div className="max-w-3xl mx-auto p-6 space-y-5">
      <header>
        <h1 className="text-lg font-bold text-white">Acompanhar solicitação</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Digite o protocolo para ver o status atualizado.
        </p>
      </header>

      <form onSubmit={(e) => { e.preventDefault(); buscar() }} className="flex gap-2 items-start">
        <Input
          placeholder="ZEL-2026-XXXXXXXX"
          className="font-mono uppercase"
          containerClassName="flex-1"
          leftIcon={<Search size={14}/>}
          value={proto}
          onChange={(e) => setProto(e.target.value.toUpperCase())}
        />
        <Button type="submit" loading={loading}>Buscar</Button>
      </form>

      {loading && (
        <Card className="p-5 space-y-3">
          <Skeleton className="h-4 w-1/3"/>
          <Skeleton className="h-3 w-1/2"/>
          <Skeleton className="h-3 w-2/3"/>
        </Card>
      )}

      {!loading && data && (
        <>
          {/* Cabeçalho */}
          <Card className="p-5 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <div className="font-mono text-[11px] text-slate-500">{data.protocolo}</div>
                <div className="text-base font-bold text-white mt-1">{data.tipoDescricao}</div>
                <div className="text-xs text-slate-400 mt-0.5">
                  {data.localizacao?.logradouro && `${data.localizacao.logradouro}, `}
                  {data.localizacao?.nomeBairro}
                </div>
              </div>
              <StatusBadge status={data.status}/>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-3 border-t border-slate-800 text-xs">
              <div>
                <div className="text-[10px] text-slate-500 uppercase tracking-wider">Aberto</div>
                <div className="text-slate-200 font-medium mt-0.5">{formatDate(data.dataAbertura)}</div>
              </div>
              {data.dataPrevisao && (
                <div>
                  <div className="text-[10px] text-slate-500 uppercase tracking-wider">Previsão</div>
                  <div className="text-slate-200 font-medium mt-0.5">{formatDate(data.dataPrevisao)}</div>
                </div>
              )}
              <div>
                <div className="text-[10px] text-slate-500 uppercase tracking-wider">SLA</div>
                <div className="text-slate-200 font-medium mt-0.5">{data.slaDias} dias</div>
              </div>
              {data.nomeServidor && (
                <div>
                  <div className="text-[10px] text-slate-500 uppercase tracking-wider">Responsável</div>
                  <div className="text-slate-200 font-medium mt-0.5 truncate">{data.nomeServidor}</div>
                </div>
              )}
            </div>
          </Card>

          {/* Stepper */}
          <Card className="p-5">
            <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4">Progresso</h3>
            <div className="flex items-center">
              {STEPS.map((step, i) => {
                const done    = i <= stepIdx
                const current = i === stepIdx
                return (
                  <div key={step} className="flex-1 flex flex-col items-center">
                    <div className="flex items-center w-full">
                      <div className={`flex-1 h-0.5 ${i === 0 ? 'opacity-0' : done ? 'bg-blue-600' : 'bg-slate-700'}`}/>
                      <div className={`
                        w-7 h-7 rounded-full flex-shrink-0 flex items-center justify-center text-[10.5px] font-bold border-2
                        ${current ? 'bg-blue-600 border-blue-300 text-white shadow-md shadow-blue-700/40'
                          : done   ? 'bg-blue-900/70 border-blue-700 text-blue-200'
                                   : 'bg-slate-800 border-slate-700 text-slate-600'}
                      `}>
                        {done && !current ? '✓' : i + 1}
                      </div>
                      <div className={`flex-1 h-0.5 ${i === STEPS.length - 1 ? 'opacity-0' : i < stepIdx ? 'bg-blue-600' : 'bg-slate-700'}`}/>
                    </div>
                    <div className={`text-[9.5px] mt-1.5 font-medium text-center
                      ${current ? 'text-blue-300' : done ? 'text-slate-300' : 'text-slate-600'}`}>
                      {LABELS[i]}
                    </div>
                  </div>
                )
              })}
            </div>
            {data.status === 'CANCELADO' && (
              <div className="mt-4 p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-xs text-red-300">
                Esta solicitação foi cancelada.
              </div>
            )}
          </Card>

          {/* Histórico */}
          {hist.length > 0 && (
            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4">Histórico</h3>
              <ol className="space-y-4">
                {hist.map((h, i) => (
                  <li key={i} className="flex gap-3">
                    <div className="flex flex-col items-center pt-0.5">
                      <div className="w-2.5 h-2.5 rounded-full bg-blue-500"/>
                      {i < hist.length - 1 && <div className="w-px flex-1 bg-slate-700 mt-1.5 min-h-4"/>}
                    </div>
                    <div className="flex-1 pb-2">
                      <div className="text-xs font-semibold text-slate-200">
                        {h.statusAnterior} <span className="text-slate-500">→</span> {h.statusNovo}
                      </div>
                      <div className="text-[10px] text-slate-500 mt-0.5">
                        {formatDateTime(h.dataHora)} · {h.nomeServidor}
                      </div>
                      {h.justificativa && (
                        <p className="text-xs text-slate-300 mt-2 px-3 py-2 rounded-lg bg-slate-800/60 border border-slate-700/50">
                          {h.justificativa}
                        </p>
                      )}
                    </div>
                  </li>
                ))}
              </ol>
            </Card>
          )}

          {/* Fotos */}
          {(data.caminhoFotoAntes || data.caminhoFotoDepois) && (
            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Fotos</h3>
              <div className="grid grid-cols-2 gap-3">
                {[
                  { src: data.caminhoFotoAntes,  label: 'Antes',  border: 'border-slate-700' },
                  { src: data.caminhoFotoDepois, label: 'Depois', border: 'border-green-700/40' },
                ].map(({ src, label, border }) => (
                  src && (
                    <div key={label} className={`rounded-xl overflow-hidden border ${border} bg-slate-900/60`}>
                      <div className="text-[10px] text-slate-500 uppercase px-3 pt-2">{label}</div>
                      {src.startsWith('data:image') || /^https?:\/\//.test(src) ? (
                        <img src={src} alt={label} className="w-full h-40 object-cover"/>
                      ) : (
                        <div className="px-3 py-3 text-xs text-slate-400 font-mono break-all">{src}</div>
                      )}
                    </div>
                  )
                ))}
              </div>
            </Card>
          )}

          {/* Avaliação */}
          {data.status === 'CONCLUIDO' && (
            <AvaliacaoForm id={data.id}/>
          )}
        </>
      )}

      {!loading && searched && !data && (
        <Card className="p-7 text-center text-slate-500 text-sm">
          Nenhuma solicitação encontrada com esse protocolo.
        </Card>
      )}
    </div>
  )
}

function AvaliacaoForm({ id }) {
  const [done, setDone] = useState(false)
  const {
    control, handleSubmit, setValue, watch,
    formState: { isSubmitting, errors },
  } = useForm({
    resolver: zodResolver(AvaliacaoSchema),
    defaultValues: { nota: 0, comentario: '' },
  })

  const nota = watch('nota')

  if (done) {
    return (
      <Card className="p-5 text-center text-sm text-green-300 bg-green-500/5 border-green-700/30">
        Obrigado pela sua avaliação! ⭐
      </Card>
    )
  }

  async function onSubmit(values) {
    try {
      await solicitacaoApi.avaliar(id, {
        nota: values.nota,
        comentario: values.comentario || null,
      })
      setDone(true)
      toast.success('Avaliação registrada. Obrigado!')
    } catch (err) {
      toast.error('Falha ao enviar', { description: err.message })
    }
  }

  return (
    <Card className="p-5">
      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">Avaliar o serviço</h3>
      <p className="text-[11px] text-slate-500 mb-4">Sua nota ajuda a melhorar o atendimento.</p>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Controller
          control={control} name="nota"
          render={() => (
            <div className="flex items-center justify-center gap-1.5">
              {[1,2,3,4,5].map(n => (
                <button key={n} type="button"
                  onClick={() => setValue('nota', n, { shouldValidate: true })}
                  className="transition-transform hover:scale-110 active:scale-95">
                  <Star size={30} className={n <= nota ? 'text-amber-400 fill-amber-400' : 'text-slate-600'}/>
                </button>
              ))}
            </div>
          )}
        />
        {errors.nota && <p className="text-[11px] text-red-400 text-center">{errors.nota.message}</p>}

        <Controller
          control={control} name="comentario"
          render={({ field }) => (
            <Textarea label="Comentário (opcional)" maxLength={500} rows={3}
              placeholder="Conte como foi o atendimento..."
              error={errors.comentario?.message}
              {...field}/>
          )}
        />

        <Button type="submit" loading={isSubmitting} disabled={!nota} className="w-full">
          Enviar avaliação
        </Button>
      </form>
    </Card>
  )
}
