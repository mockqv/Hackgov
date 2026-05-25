import { useEffect, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { ArrowLeft, CheckCircle2, Copy, Send } from 'lucide-react'

import { solicitacaoApi, tipoServicoApi } from '../../lib/api'
import { SolicitacaoSchema } from '../../lib/validation'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { Input, Select, Textarea } from '../../components/ui/Input'
import LocationPicker from '../../components/map/LocationPicker'
import CepInput from '../../components/forms/CepInput'
import PhotoUpload from '../../components/forms/PhotoUpload'

export default function NovaSolicitacao() {
  const navigate = useNavigate()
  const [tipos, setTipos] = useState([])
  const [protocolo, setProtocolo] = useState(null)

  const {
    register, handleSubmit, control, watch, setValue,
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
  }, [])

  const latitude  = watch('latitude')
  const longitude = watch('longitude')

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
          <div className="w-14 h-14 mx-auto rounded-2xl bg-green-500/10 border border-green-500/30 flex items-center justify-center">
            <CheckCircle2 size={28} className="text-green-400"/>
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">Solicitação enviada!</h2>
            <p className="text-slate-400 text-sm mt-1.5">
              Seu chamado foi registrado e já está no mapa público.
            </p>
          </div>
          <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-4 space-y-2">
            <div className="text-[10px] text-slate-500 uppercase tracking-wider">Protocolo</div>
            <div className="font-mono text-lg font-bold text-blue-400 tracking-wider">{protocolo}</div>
            <button
              onClick={() => navigator.clipboard.writeText(protocolo).then(() => toast.success('Copiado!'))}
              className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200">
              <Copy size={11}/> Copiar
            </button>
          </div>
          <div className="flex gap-2.5">
            <Button variant="secondary" className="flex-1"
              onClick={() => { setProtocolo(null); reset() }}>
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
          <h1 className="text-lg font-bold text-white">Nova solicitação</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Reporte um problema urbano para a prefeitura resolver.
          </p>
        </div>
      </header>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {/* Tipo + Descrição */}
        <Card className="p-5 space-y-4">
          <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Sobre o problema</h2>
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
        </Card>

        {/* Localização */}
        <Card className="p-5 space-y-4">
          <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Localização</h2>

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
                error={errors.latitude?.message || errors.longitude?.message}
              />
            )}
          />

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <Controller
              control={control} name="cep"
              render={({ field }) => (
                <CepInput
                  value={field.value}
                  onChange={field.onChange}
                  onResolve={(addr) => {
                    if (addr.logradouro) setValue('logradouro', addr.logradouro)
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

          <Input label="Bairro (ID)" type="number" min={1} required
            hint="Use 1 enquanto a seleção por nome não está disponível."
            error={errors.bairroId?.message}
            {...register('bairroId')} />
        </Card>

        {/* Foto */}
        <Card className="p-5">
          <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Foto do problema</h2>
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
