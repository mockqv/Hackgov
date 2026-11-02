import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import {
  AlertTriangle, CheckCircle2, Clock, Fingerprint, Link2, RefreshCw, Search, ShieldCheck, XCircle,
} from 'lucide-react'

import useApi from '../../hooks/useApi'
import { integridadeApi } from '../../lib/api'
import { formatDateTime } from '../../lib/format'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'
import { Input } from '../../components/ui/Input'

const curto = (h) => (h ? `${h.slice(0, 10)}…${h.slice(-6)}` : '—')
const dataBr = (d) => d?.split('-').reverse().join('/')

const SITUACAO = {
  INTEGRO:    { rotulo: 'Íntegro',    classe: 'bg-emerald-50 text-emerald-700 border-emerald-200', icone: CheckCircle2 },
  DIVERGENTE: { rotulo: 'Divergente', classe: 'bg-rose-50 text-rose-700 border-rose-200',          icone: XCircle },
  PENDENTE:   { rotulo: 'Pendente',   classe: 'bg-slate-100 text-slate-600 border-slate-200',      icone: Clock },
}

function Selo({ situacao }) {
  const s = SITUACAO[situacao] || SITUACAO.PENDENTE
  const Icone = s.icone
  return (
    <span className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${s.classe}`}>
      <Icone size={11} /> {s.rotulo}
    </span>
  )
}

function Kpi({ rotulo, valor, cor = 'text-slate-900' }) {
  return (
    <Card className="p-4">
      <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{rotulo}</div>
      <div className={`text-2xl font-black font-display ${cor}`}>{valor}</div>
    </Card>
  )
}

/**
 * Tela "Integridade" (Fase 7, Partes 1 e 4) — servidor e gestor.
 * O back-end recalcula a raiz de Merkle de cada dia da trilha de auditoria (HISTORICO_STATUS)
 * e compara com a raiz ancorada no contrato HackGovRegistro. Se um evento for alterado no banco
 * depois da ancoragem, o dia aparece DIVERGENTE e a prova do evento deixa de conferir.
 */
export default function Integridade() {
  const lotes = useApi(integridadeApi.lotes, [])
  const [todos, setTodos] = useState(false)
  const [protocolo, setProtocolo] = useState('')
  const [prova, setProva] = useState(null)
  const [buscando, setBuscando] = useState(false)

  const r = lotes.data
  const lista = r?.lotes || []
  const divergentes = lista.filter(l => l.situacao === 'DIVERGENTE')

  useEffect(() => {
    if (!protocolo && lista.length) {
      const exemplo = lista.find(l => l.situacao !== 'PENDENTE' && l.exemploProtocolo) || lista.find(l => l.exemploProtocolo)
      if (exemplo) setProtocolo(exemplo.exemploProtocolo)
    }
  }, [lista, protocolo])

  async function verificar() {
    await lotes.refresh()
    toast.success('Raízes recalculadas a partir do banco atual')
  }

  async function consultarProva(e) {
    e?.preventDefault()
    if (!protocolo.trim()) return
    setBuscando(true)
    try {
      const res = await integridadeApi.prova(protocolo.trim())
      setProva(res.data)
    } catch (err) {
      setProva(null)
      toast.error(err?.response?.data?.message || 'Protocolo não encontrado')
    } finally {
      setBuscando(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-bold font-display text-slate-900 flex items-center gap-2"><ShieldCheck size={18} /> Integridade da auditoria</h1>
          <p className="text-xs text-slate-500 mt-0.5">Trilha de status ancorada em blockchain (contrato HackGovRegistro)</p>
        </div>
        <Button size="sm" onClick={verificar} loading={lotes.loading}>
          <RefreshCw size={12} /> Verificar agora
        </Button>
      </header>

      <Card className="p-4 grid md:grid-cols-3 gap-4 text-xs text-slate-600 leading-relaxed">
        <p><b className="text-slate-800">1. Cada evento vira um hash.</b> Toda mudança de status (quem, quando, de/para, justificativa)
          gera um SHA-256. Mudar um caractere muda o hash inteiro.</p>
        <p><b className="text-slate-800">2. Um dia vira uma raiz.</b> Os hashes do dia formam uma árvore de Merkle; só a raiz vai para a
          blockchain — uma transação por dia, sem nenhum dado pessoal.</p>
        <p><b className="text-slate-800">3. Qualquer alteração aparece.</b> O sistema recalcula a raiz a partir do banco e compara com a
          ancorada. Nem o administrador do banco consegue mudar o passado sem ser notado.</p>
      </Card>

      {lotes.loading && !r ? (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">{[1, 2, 3, 4, 5].map(i => <Skeleton key={i} className="h-20 rounded-2xl" />)}</div>
      ) : lotes.error ? (
        <ErrorState onRetry={lotes.refresh} />
      ) : !lista.length ? (
        <EmptyState icon={ShieldCheck} title="Sem eventos de auditoria" description="Os dias aparecem aqui assim que houver mudanças de status." />
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            <Kpi rotulo="Dias auditados" valor={r.dias} />
            <Kpi rotulo="Eventos" valor={r.eventos.toLocaleString('pt-BR')} />
            <Kpi rotulo="Íntegros" valor={r.integros} cor="text-emerald-600" />
            <Kpi rotulo="Divergentes" valor={r.divergentes} cor={r.divergentes ? 'text-rose-600' : 'text-slate-900'} />
            <Kpi rotulo="Pendentes" valor={r.pendentes} cor="text-slate-500" />
          </div>

          {divergentes.length > 0 && (
            <Card className="p-4 border-rose-200 bg-rose-50">
              <div className="flex items-start gap-3 text-sm text-rose-800">
                <AlertTriangle size={18} className="shrink-0 mt-0.5" />
                <div>
                  <b>Alteração detectada na trilha de auditoria.</b> A raiz recalculada não confere com a ancorada em{' '}
                  {divergentes.map(l => dataBr(l.data)).join(', ')}. Algum evento desses dias foi modificado ou apagado depois
                  da ancoragem. Consulte a prova de um protocolo do dia para localizar o evento.
                </div>
              </div>
            </Card>
          )}
          {r.pendentes > 0 && r.ancorados === 0 && (
            <Card className="p-4 text-xs text-slate-600">
              Nenhum dia ancorado ainda. Rode <code className="rounded bg-slate-100 px-1">npm run demo -- --api http://localhost:8081</code> na
              pasta <code className="rounded bg-slate-100 px-1">blockchain/</code> para gravar as raízes no contrato.
            </Card>
          )}

          <Card className="p-0 overflow-hidden">
            <table className="w-full text-xs">
              <thead className="bg-slate-50 text-[10px] uppercase tracking-wider text-slate-500">
                <tr className="text-left">
                  <th className="px-4 py-2">Dia</th><th>Eventos</th><th>Raiz atual (banco)</th><th>Situação</th><th>Transação</th><th>Lote</th>
                </tr>
              </thead>
              <tbody>
                {(todos ? lista : lista.slice(0, 12)).map(l => (
                  <tr key={l.data} className={`border-t border-slate-100 ${l.situacao === 'DIVERGENTE' ? 'bg-rose-50/60' : ''}`}>
                    <td className="px-4 py-2 font-semibold text-slate-800">{dataBr(l.data)}</td>
                    <td className="text-slate-600">{l.eventos}</td>
                    <td className="font-mono text-[11px] text-slate-600" title={l.raizAtual}>{curto(l.raizAtual)}</td>
                    <td><Selo situacao={l.situacao} /></td>
                    <td className="font-mono text-[11px] text-slate-500" title={l.txHash || ''}>{curto(l.txHash)}</td>
                    <td className="text-slate-500">{l.indiceLote ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {lista.length > 12 && (
              <button className="w-full border-t border-slate-100 py-2 text-xs font-semibold text-blue-600 hover:bg-slate-50" onClick={() => setTodos(v => !v)}>
                {todos ? 'Mostrar só os 12 mais recentes' : `Mostrar os ${lista.length} dias`}
              </button>
            )}
          </Card>
        </>
      )}

      <Card className="p-5 space-y-4">
        <div>
          <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2"><Fingerprint size={15} /> Prova de um chamado</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Para cada evento: o hash do evento (folha), os hashes irmãos até a raiz (O(log n)) e se a prova bate com a raiz ancorada.
            É a mesma prova que o contrato confere em <code>verificarInclusao</code>.
          </p>
        </div>
        <form onSubmit={consultarProva} className="flex gap-2">
          <Input value={protocolo} onChange={e => setProtocolo(e.target.value)} placeholder="ZEL-2026-XXXXXXXX" className="font-mono" />
          <Button type="submit" loading={buscando}><Search size={13} /> Consultar</Button>
        </form>
        {prova && (
          <div className="space-y-2">
            <div className="text-[11px] text-slate-500">
              Na blockchain o protocolo aparece só como <span className="font-mono">{curto(prova.hashProtocolo)}</span> (SHA-256).
            </div>
            {prova.eventos.map(ev => (
              <div key={ev.idHistorico} className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-xl border border-slate-200 px-3 py-2 text-xs">
                <span className="font-semibold text-slate-800 w-56">{ev.statusAnterior} → {ev.statusNovo}</span>
                <span className="text-slate-500 w-32">{formatDateTime(ev.dataHora)}</span>
                <span className="font-mono text-[11px] text-slate-500" title={ev.folha}>folha {curto(ev.folha)}</span>
                <span className="text-slate-500">{ev.prova.length} hashes na prova</span>
                {ev.raizAncorada == null ? (
                  <span className="ml-auto text-slate-400 flex items-center gap-1"><Clock size={12} /> dia ainda não ancorado</span>
                ) : ev.provaConfere ? (
                  <span className="ml-auto text-emerald-700 font-semibold flex items-center gap-1"><Link2 size={12} /> confere com a âncora</span>
                ) : (
                  <span className="ml-auto text-rose-700 font-semibold flex items-center gap-1"><XCircle size={12} /> NÃO confere: evento alterado</span>
                )}
              </div>
            ))}
            {!prova.eventos.length && <div className="text-xs text-slate-500">Este chamado ainda não tem mudanças de status.</div>}
          </div>
        )}
      </Card>
    </div>
  )
}
