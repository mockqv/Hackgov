import { useEffect, useRef, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AlertTriangle, ArrowLeft, CheckCircle2, Copy, Loader2, Send, Sparkles } from 'lucide-react'

import {
  solicitacaoApi, tipoServicoApi, bairroApi, iaApi, nominatimReverse, nominatimForward,
} from '../../lib/api'
import { SolicitacaoSchema } from '../../lib/validation'
import { formatProbabilidade } from '../../lib/format'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StatusBadge from '../../components/ui/StatusBadge'
import { Input, Select, Textarea } from '../../components/ui/Input'
import LocationPicker from '../../components/map/LocationPicker'
import CepInput from '../../components/forms/CepInput'
import PhotoUpload from '../../components/forms/PhotoUpload'

const RAIO_AVISO_M = 80

const normalizar = (s = '') => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()

export default function NovaSolicitacao() {
  const navigate = useNavigate()
  const [tipos, setTipos] = useState([])
  const [bairros, setBairros] = useState([])
  const [protocolo, setProtocolo] = useState(null)

  // Fase 6 — IA de triagem e aviso de chamados próximos
  const [sugestao, setSugestao] = useState(null)
  const [analisando, setAnalisando] = useState(false)
  const [proximas, setProximas] = useState([])
  const requisicaoIa = useRef(0)

  const {
    register, handleSubmit, control, watch, setValue, getValues,
    formState: { errors, isSubmitting },
    reset,
  } = useForm({
    resolver: zodResolver(SolicitacaoSchema),
    defaultValues: {
      tipoServicoId: '', descricao: '',
      latitude: null, longitude: null,
      logradouro: '', numero: '', complemento: '', cep: '',
      bairroId: 1, caminhoFotoAntes: '',
    },
  })

  useEffect(() => {
    tipoServicoApi.listar()
      .then(r => setTipos(r?.data || []))
      .catch(() => {})
    bairroApi.listar()
      .then(r => {
        const lista = r?.data || []
        setBairros(lista)
        // sincroniza o <select> com o valor do formulário depois que as opções chegam
        const atual = getValues('bairroId')
        const existe = lista.some(b => String(b.id) === String(atual))
        if (lista.length) setValue('bairroId', String(existe ? atual : lista[0].id))
      })
      .catch(() => {})
  }, [getValues, setValue])

  const latitude      = watch('latitude')
  const longitude     = watch('longitude')
  const descricao     = watch('descricao')
  const tipoServicoId = watch('tipoServicoId')

  // IA: classifica a descrição enquanto o cidadão digita (debounce de 700 ms)
  useEffect(() => {
    const texto = (descricao || '').trim()
    if (texto.length < 12) {
      setSugestao(null)
      setAnalisando(false)
      return
    }
    const id = ++requisicaoIa.current
    setAnalisando(true)
    const timer = setTimeout(async () => {
      try {
        const r = await iaApi.classificar(texto)
        if (id === requisicaoIa.current) setSugestao(r.data)
      } catch {
        if (id === requisicaoIa.current) setSugestao(null) // IA é apoio: falhou, o formulário segue normal
      } finally {
        if (id === requisicaoIa.current) setAnalisando(false)
      }
    }, 700)
    return () => clearTimeout(timer)
  }, [descricao])

  // Aviso de possível duplicidade: abertos do mesmo tipo a até 80 m do pino
  useEffect(() => {
    if (latitude == null || longitude == null || !tipoServicoId) {
      setProximas([])
      return
    }
    const timer = setTimeout(() => {
      solicitacaoApi.proximas({ latitude, longitude, tipoServicoId: Number(tipoServicoId), raioMetros: RAIO_AVISO_M })
        .then(r => setProximas(r?.data || []))
        .catch(() => setProximas([]))
    }, 500)
    return () => clearTimeout(timer)
  }, [latitude, longitude, tipoServicoId])

  function selecionarBairroPorNome(nome) {
    if (!nome || bairros.length === 0) return
    const alvo = normalizar(nome)
    const achado = bairros.find(b => normalizar(b.nome) === alvo)
      || bairros.find(b => alvo.includes(normalizar(b.nome)) || normalizar(b.nome).includes(alvo))
    if (achado) setValue('bairroId', String(achado.id), { shouldValidate: true })
  }

  async function onSubmit(values) {
    if (values.latitude == null || values.longitude == null) {
      toast.error('Defina o local no mapa antes de enviar')
      return
    }
    try {
      const payload = {
        tipoServicoId:    Number(values.tipoServicoId),
        descricao:        values.descricao.trim(),
        latitude:         values.latitude,
        longitude:        values.longitude,
        logradouro:       values.logradouro || null,
        numero:           values.numero     || null,
        complemento:      values.complemento|| null,
        cep:              values.cep        || null,
        bairroId:         Number(values.bairroId),
        caminhoFotoAntes: values.caminhoFotoAntes || null,
      }
      const res = await solicitacaoApi.criar(payload)
      setProtocolo(res.data.protocolo)
      toast.success('Solicitação criada!', { description: res.data.protocolo })
    } catch (err) {
      toast.error('Falha ao criar', { description: err.message })
    }
  }

  // ── Tela de sucesso ─────────────────────────────────────
  if (protocolo) {
    return (
      <div className="max-w-md mx-auto p-6 mt-6">
        <Card className="p-7 text-center space-y-5">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center">
            <CheckCircle2 size={28} className="text-emerald-600"/>
          </div>
          <div>
            <h2 className="text-lg font-bold font-display text-slate-900">Solicitação enviada!</h2>
            <p className="text-slate-500 text-sm mt-1.5">
              Seu chamado foi registrado e já está no mapa público.
            </p>
          </div>
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-2">
            <div className="text-[10px] text-slate-500 uppercase tracking-wider">Protocolo</div>
            <div className="font-mono text-lg font-bold text-blue-600 tracking-wider">{protocolo}</div>
            <button
              onClick={() => navigator.clipboard.writeText(protocolo).then(() => toast.success('Copiado!'))}
              className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-700">
              <Copy size={11}/> Copiar
            </button>
          </div>
          <div className="flex gap-2.5">
            <Button variant="secondary" className="flex-1"
              onClick={() => { setProtocolo(null); setSugestao(null); setProximas([]); reset() }}>
              Outra solicitação
            </Button>
            <Button className="flex-1"
              onClick={() => navigate(`/acompanhar/${protocolo}`)}>
              Acompanhar
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  // ── Formulário ──────────────────────────────────────────
  return (
    <div className="max-w-3xl mx-auto p-6">
      <header className="flex items-center gap-3 mb-6">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ArrowLeft size={15}/>
        </Button>
        <div>
          <h1 className="text-lg font-bold font-display text-slate-900">Nova solicitação</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Reporte um problema urbano para a prefeitura resolver.
          </p>
        </div>
      </header>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {/* Tipo + Descrição */}
        <Card className="p-5 space-y-4">
          <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Sobre o problema</h2>
          <Select
            label="Tipo de serviço"
            required
            error={errors.tipoServicoId?.message}
            {...register('tipoServicoId')}>
            <option value="">Selecione...</option>
            {tipos.map(t => (
              <option key={t.id} value={t.id}>{t.descricao} — SLA {t.slaDias} dias</option>
            ))}
          </Select>

          <Textarea
            label="Descrição"
            required
            placeholder="Descreva o problema em detalhes (mínimo 10 caracteres)..."
            maxLength={500}
            rows={4}
            error={errors.descricao?.message}
            hint={`Quanto mais contexto, melhor a triagem.`}
            {...register('descricao')}
          />

          <SugestaoIa
            sugestao={sugestao}
            analisando={analisando}
            tipoSelecionado={tipoServicoId}
            onUsar={() => setValue('tipoServicoId', String(sugestao.tipoServicoId), { shouldValidate: true })}
          />
        </Card>

        {/* Localização */}
        <Card className="p-5 space-y-4">
          <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Localização</h2>

          <Controller
            control={control}
            name="latitude"
            render={() => (
              <LocationPicker
                value={latitude != null && longitude != null ? { lat: latitude, lng: longitude } : null}
                onChange={({ lat, lng }) => {
                  setValue('latitude',  lat, { shouldValidate: true })
                  setValue('longitude', lng, { shouldValidate: true })
                }}
                onUserMove={async ({ lat, lng }) => {
                  // Reverse geocode: pin → endereço (e bairro do catálogo, quando o nome bate)
                  const addr = await nominatimReverse({ lat, lng })
                  if (!addr) return
                  if (addr.logradouro) setValue('logradouro', addr.logradouro, { shouldValidate: true })
                  if (addr.cep)        setValue('cep',        addr.cep,        { shouldValidate: true })
                  selecionarBairroPorNome(addr.bairro)
                  if (addr.bairro || addr.cidade) {
                    toast.success('Endereço preenchido', {
                      description: [addr.logradouro, addr.bairro, addr.cidade].filter(Boolean).join(' · '),
                    })
                  }
                }}
                error={errors.latitude?.message || errors.longitude?.message}
              />
            )}
          />

          <AvisoProximas itens={proximas}/>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <Controller
              control={control} name="cep"
              render={({ field }) => (
                <CepInput
                  value={field.value}
                  onChange={field.onChange}
                  onResolve={async (addr) => {
                    if (addr.logradouro) setValue('logradouro', addr.logradouro, { shouldValidate: true })
                    selecionarBairroPorNome(addr.bairro)
                    // Forward geocode: CEP → mover pin
                    const coords = await nominatimForward({
                      cep: field.value,
                      logradouro: addr.logradouro,
                      cidade: addr.cidade,
                      uf: addr.uf,
                    })
                    if (coords) {
                      setValue('latitude',  coords.lat, { shouldValidate: true })
                      setValue('longitude', coords.lng, { shouldValidate: true })
                    }
                  }}
                  error={errors.cep?.message}
                />
              )}
            />
            <Input label="Logradouro" placeholder="Rua das Flores"
              containerClassName="md:col-span-2"
              error={errors.logradouro?.message}
              {...register('logradouro')} />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <Input label="Número" placeholder="100"
              error={errors.numero?.message} {...register('numero')} />
            <Input label="Complemento" placeholder="Apto, bloco..."
              containerClassName="md:col-span-2"
              error={errors.complemento?.message} {...register('complemento')} />
          </div>

          <Select label="Bairro" required
            hint="Selecionado automaticamente quando o pino ou o CEP indicam um bairro atendido."
            error={errors.bairroId?.message}
            {...register('bairroId')}>
            {bairros.length === 0 && <option value="1">Centro</option>}
            {bairros.map(b => (
              <option key={b.id} value={b.id}>{b.nome}{b.municipio ? ` — ${b.municipio}/${b.uf}` : ''}</option>
            ))}
          </Select>
        </Card>

        {/* Foto */}
        <Card className="p-5">
          <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">Foto do problema</h2>
          <Controller
            control={control} name="caminhoFotoAntes"
            render={({ field }) => (
              <PhotoUpload value={field.value} onChange={field.onChange} error={errors.caminhoFotoAntes?.message}/>
            )}
          />
        </Card>

        <Button type="submit" loading={isSubmitting} size="lg" className="w-full">
          <Send size={15}/> Enviar solicitação
        </Button>
      </form>
    </div>
  )
}

/** Cartão com a sugestão da IA de triagem — o cidadão decide se usa ou não. */
function SugestaoIa({ sugestao, analisando, tipoSelecionado, onUsar }) {
  if (!sugestao && !analisando) return null

  if (!sugestao) {
    return (
      <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-[11px] text-slate-500">
        <Loader2 size={13} className="animate-spin"/> A IA está analisando a descrição...
      </div>
    )
  }
  if (!sugestao.disponivel) return null

  const pct = formatProbabilidade(sugestao.confianca)
  if (!sugestao.confiavel) {
    return (
      <div className="flex items-start gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-[11px] text-slate-500">
        <Sparkles size={13} className="mt-0.5 shrink-0"/>
        <span>
          A IA não tem confiança suficiente para sugerir um tipo
          {sugestao.codigo ? ` (melhor palpite: ${sugestao.tipoDescricao}, ${pct})` : ''}. Escolha o tipo manualmente.
        </span>
      </div>
    )
  }

  const jaSelecionado = String(tipoSelecionado) === String(sugestao.tipoServicoId)
  return (
    <div className="rounded-xl border border-blue-200 bg-blue-50/70 px-3.5 py-3">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="w-8 h-8 rounded-lg bg-white border border-blue-200 flex items-center justify-center shrink-0">
            <Sparkles size={15} className="text-blue-600"/>
          </div>
          <div className="min-w-0">
            <div className="text-[10px] font-semibold uppercase tracking-wider text-blue-700">Sugestão da IA de triagem</div>
            <div className="text-sm font-bold text-slate-900 truncate">
              {sugestao.tipoDescricao}
              <span className="ml-1.5 text-xs font-semibold text-blue-700">{pct} de confiança</span>
            </div>
          </div>
        </div>
        {jaSelecionado ? (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 shrink-0">
            <CheckCircle2 size={13}/> Tipo selecionado
          </span>
        ) : (
          <Button type="button" size="sm" variant="outline" className="shrink-0" onClick={onUsar}>
            Usar sugestão
          </Button>
        )}
      </div>
      {sugestao.termosRelevantes?.length > 0 && (
        <div className="mt-2.5 flex flex-wrap items-center gap-1.5 text-[10.5px] text-slate-600">
          <span>Termos que mais pesaram:</span>
          {sugestao.termosRelevantes.map(t => (
            <span key={t} className="px-1.5 py-0.5 rounded-md bg-white border border-blue-200 text-blue-800 font-medium">{t}</span>
          ))}
        </div>
      )}
    </div>
  )
}

/** Aviso de possível duplicidade: chamados abertos do mesmo tipo perto do pino. */
function AvisoProximas({ itens }) {
  if (!itens?.length) return null
  return (
    <div className="rounded-xl border border-amber-200 bg-amber-50 px-3.5 py-3">
      <div className="flex items-start gap-2.5">
        <AlertTriangle size={15} className="text-amber-600 mt-0.5 shrink-0"/>
        <div className="min-w-0">
          <div className="text-xs font-semibold text-amber-900">
            {itens.length === 1
              ? `Já existe um chamado aberto deste tipo a menos de ${RAIO_AVISO_M} m`
              : `Já existem ${itens.length} chamados abertos deste tipo a menos de ${RAIO_AVISO_M} m`}
          </div>
          <p className="text-[11px] text-amber-800 mt-0.5">
            Pode ser o mesmo problema — você pode acompanhar o protocolo existente em vez de abrir outro.
          </p>
          <ul className="mt-2 space-y-1.5">
            {itens.slice(0, 3).map(p => (
              <li key={p.protocolo} className="flex flex-wrap items-center gap-2 text-[11px] text-amber-900">
                <Link to={`/acompanhar/${p.protocolo}`} className="font-mono font-semibold hover:underline">{p.protocolo}</Link>
                <span>a {Math.round(p.distanciaMetros)} m</span>
                <StatusBadge status={p.status}/>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
