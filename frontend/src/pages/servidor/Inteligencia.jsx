import { useState } from 'react'
import { toast } from 'sonner'
import { Brain, Gauge, Layers, MapPinned, Network, RefreshCw, Sparkles, Target, Timer } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { analiseApi, iaApi } from '../../lib/api'
import { formatDate, formatProbabilidade } from '../../lib/format'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'
import { Textarea } from '../../components/ui/Input'
import CityMap from '../../components/map/CityMap'

const NOMES_CLASSE = {
  BURACO: 'Buraco', ILUMINACAO: 'Iluminação', PODA: 'Poda', ENTULHO: 'Entulho', OUTROS: 'Outros',
}
const pct = (v, casas = 1) => (v == null ? '—' : `${(v * 100).toFixed(casas).replace('.', ',')}%`)

function Kpi({ icon: Icon, label, value, hint }) {
  return (
    <Card className="p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{label}</span>
        <div className="w-8 h-8 rounded-lg border border-blue-200 bg-blue-50 text-blue-600 flex items-center justify-center">
          <Icon size={15}/>
        </div>
      </div>
      <div className="text-2xl font-black font-display text-slate-900">{value}</div>
      {hint && <div className="text-[11px] text-slate-500 mt-0.5">{hint}</div>}
    </Card>
  )
}

/**
 * Tela "Inteligência" (Fase 6) — servidor e gestor:
 *  1. ficha técnica e métricas da IA de triagem (Naive Bayes) + teste ao vivo;
 *  2. pontos críticos e possíveis duplicados (KD-Tree + grafo/Union-Find no backend).
 */
export default function Inteligencia() {
  const modelo = useApi(iaApi.modelo, [])

  const [escopo, setEscopo] = useState('historico') // 'historico' = pontos críticos · 'abertas' = duplicados
  const [raio, setRaio] = useState(50)
  const [selecionado, setSelecionado] = useState(null)
  const agrupamentos = useApi(() => analiseApi.agrupamentos({
    raioMetros: raio,
    apenasAbertas: escopo === 'abertas',
    minimo: escopo === 'abertas' ? 2 : 4,
    limite: 10,
  }), [raio, escopo])

  const m = modelo.data
  const cv = m?.validacaoCruzada
  const grupos = agrupamentos.data?.grupos || []
  const pinos = grupos.map((g, i) => ({
    id: i,
    latitude: g.latitude,
    longitude: g.longitude,
    status: g.abertas > 0 ? 'RECEBIDO' : 'AGENDADO',
    protocolo: `${g.quantidade} chamados`,
    tipoDescricao: g.tipoDescricao,
    logradouro: [g.logradouro, g.bairro].filter(Boolean).join(' · '),
  }))

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-bold font-display text-slate-900">Inteligência</h1>
          <p className="text-xs text-slate-500 mt-0.5">IA de triagem e análise espacial dos chamados</p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => { modelo.refresh(); agrupamentos.refresh() }}>
          <RefreshCw size={12} className={modelo.loading || agrupamentos.loading ? 'animate-spin' : ''}/> Atualizar
        </Button>
      </header>

      {/* ── IA de triagem ─────────────────────────────────── */}
      <section className="space-y-4">
        <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
          <Brain size={13}/> IA de triagem — classificador Naive Bayes (roda no próprio servidor)
        </h2>

        {modelo.loading && !m ? (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">{[1,2,3,4].map(i => <Skeleton key={i} className="h-24 rounded-2xl"/>)}</div>
        ) : modelo.error ? (
          <ErrorState onRetry={modelo.refresh}/>
        ) : !m?.disponivel ? (
          <EmptyState icon={Brain} title="IA indisponível" description="O modelo não pôde ser carregado; o sistema segue sem sugestões automáticas."/>
        ) : (
          <>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <Kpi icon={Target} label="Acurácia" value={pct(cv?.acuracia)} hint={`validação cruzada ${cv?.folds}-fold`}/>
              <Kpi icon={Gauge} label="F1 macro" value={pct(cv?.f1Macro)} hint="média entre os 5 tipos"/>
              <Kpi icon={Layers} label="Exemplos de treino" value={m.exemplosTreino} hint={`${m.vocabulario} termos no vocabulário`}/>
              <Kpi icon={Sparkles} label="Limiar de sugestão" value={pct(m.limiarConfianca, 0)} hint="abaixo disso a IA não sugere"/>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <TestarClassificador/>
              <MatrizConfusao cv={cv}/>
            </div>

            <CurvaLimiar curva={cv?.curvaLimiar || []} limiar={m.limiarConfianca}/>
          </>
        )}
      </section>

      {/* ── Análise espacial ──────────────────────────────── */}
      <section className="space-y-4 pt-2">
        <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
          <Network size={13}/> Análise espacial — KD-Tree + grafo de proximidade
        </h2>

        <Card className="p-5 space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            {[
              { v: 'historico', l: 'Pontos críticos (histórico, ≥ 4 chamados)' },
              { v: 'abertas',   l: 'Possíveis duplicados (só abertas, ≥ 2)' },
            ].map(o => (
              <button key={o.v} onClick={() => { setEscopo(o.v); setSelecionado(null) }}
                className={`px-3 py-1.5 rounded-full text-xs font-semibold border transition-colors cursor-pointer
                  ${escopo === o.v ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-white border-slate-200 text-slate-500 hover:bg-slate-50'}`}>
                {o.l}
              </button>
            ))}
            <span className="mx-1 h-5 w-px bg-slate-200"/>
            <span className="text-[11px] text-slate-500">Raio:</span>
            {[30, 50, 100].map(r => (
              <button key={r} onClick={() => { setRaio(r); setSelecionado(null) }}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold border cursor-pointer
                  ${raio === r ? 'bg-slate-900 border-slate-900 text-white' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
                {r} m
              </button>
            ))}
          </div>

          {agrupamentos.error ? (
            <ErrorState onRetry={agrupamentos.refresh}/>
          ) : (
            <>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {[
                  { icon: MapPinned, l: 'Chamados analisados', v: agrupamentos.data?.pontos ?? '—' },
                  { icon: Network,   l: 'Grupos encontrados',  v: agrupamentos.data?.totalGrupos ?? '—' },
                  { icon: Timer,     l: 'Tempo do algoritmo',  v: agrupamentos.data ? `${String(agrupamentos.data.tempoMs).replace('.', ',')} ms` : '—' },
                  { icon: Layers,    l: 'Complexidade',        v: 'O(n log n)' },
                ].map(s => (
                  <div key={s.l} className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5">
                    <div className="flex items-center gap-1.5 text-[10px] uppercase tracking-wider text-slate-500 font-semibold">
                      <s.icon size={11}/> {s.l}
                    </div>
                    <div className="text-lg font-black text-slate-900 mt-0.5">{s.v}</div>
                  </div>
                ))}
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-5 gap-4">
                <div className="lg:col-span-3 h-[360px]">
                  <CityMap items={pinos} zoom={11} ajustarAosPontos selectedId={selecionado}
                    onSelect={(p) => setSelecionado(p.id)} height="360px"/>
                </div>
                <div className="lg:col-span-2 max-h-[360px] overflow-y-auto pr-1">
                  {agrupamentos.loading && !agrupamentos.data ? (
                    <div className="space-y-2">{[1,2,3,4].map(i => <Skeleton key={i} className="h-16"/>)}</div>
                  ) : grupos.length === 0 ? (
                    <EmptyState icon={Network} title="Nenhum grupo" description="Nenhum conjunto de chamados próximos com esse raio."/>
                  ) : (
                    <ol className="space-y-2">
                      {grupos.map((g, i) => (
                        <li key={i}>
                          <button onClick={() => setSelecionado(i)}
                            className={`w-full text-left rounded-xl border px-3 py-2.5 transition-colors cursor-pointer
                              ${selecionado === i ? 'border-blue-300 bg-blue-50' : 'border-slate-200 bg-white hover:bg-slate-50'}`}>
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-xs font-bold text-slate-900 truncate">{i + 1}. {g.tipoDescricao}</span>
                              <span className="text-xs font-black text-blue-700 shrink-0">{g.quantidade} chamados</span>
                            </div>
                            <div className="text-[11px] text-slate-500 mt-0.5 truncate">
                              {[g.logradouro, g.bairro].filter(Boolean).join(' · ')}
                            </div>
                            <div className="text-[10px] text-slate-500 mt-1">
                              {formatDate(g.primeiraAbertura)} → {formatDate(g.ultimaAbertura)}
                              {g.abertas > 0 && <span className="ml-1.5 text-red-600 font-semibold">· {g.abertas} em aberto</span>}
                            </div>
                          </button>
                        </li>
                      ))}
                    </ol>
                  )}
                </div>
              </div>
              <p className="text-[11px] text-slate-500">
                Cada chamado é um vértice; dois chamados do mesmo tipo a até {raio} m formam uma aresta. Os grupos são os
                componentes conexos do grafo — encontrados com uma KD-Tree (busca por raio) e Union-Find.
                {escopo === 'historico'
                  ? ' Grupos grandes indicam reincidência: vale uma obra definitiva no trecho em vez de reparos repetidos.'
                  : ' Grupos de chamados abertos indicam o mesmo problema registrado por vizinhos diferentes.'}
              </p>
            </>
          )}
        </Card>
      </section>
    </div>
  )
}

/** Caixa de teste ao vivo: digita um texto e vê a decisão da IA, com as probabilidades. */
function TestarClassificador() {
  const [texto, setTexto] = useState('O poste da minha rua está apagado há uma semana e fica tudo escuro')
  const [resultado, setResultado] = useState(null)
  const [carregando, setCarregando] = useState(false)

  async function classificar() {
    if (texto.trim().length < 3) return
    setCarregando(true)
    try {
      const r = await iaApi.classificar(texto.trim())
      setResultado(r.data)
    } catch (err) {
      toast.error('Falha ao classificar', { description: err.message })
    } finally {
      setCarregando(false)
    }
  }

  return (
    <Card className="p-5 space-y-3">
      <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Testar o classificador</h3>
      <Textarea rows={3} maxLength={500} value={texto} onChange={(e) => setTexto(e.target.value)}
        placeholder="Digite a descrição de um chamado..."/>
      <Button size="sm" onClick={classificar} loading={carregando}><Sparkles size={13}/> Classificar</Button>

      {resultado && (
        resultado.codigo ? (
          <div className="space-y-2 pt-1">
            <div className="text-sm text-slate-700">
              <span className="font-bold text-slate-900">{resultado.tipoDescricao}</span>
              {' '}— confiança {formatProbabilidade(resultado.confianca, 1)}
              {!resultado.confiavel && <span className="ml-1 text-amber-600 text-xs font-semibold">(abaixo do limiar: não seria sugerido)</span>}
            </div>
            {resultado.alternativas.map(a => (
              <div key={a.codigo} className="flex items-center gap-2">
                <span className="text-[11px] text-slate-600 w-28 truncate">{a.descricao}</span>
                <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div className="h-full rounded-full bg-blue-600" style={{ width: `${Math.max(1, a.probabilidade * 100)}%` }}/>
                </div>
                <span className="text-[11px] text-slate-500 w-14 text-right font-mono">{formatProbabilidade(a.probabilidade, 1)}</span>
              </div>
            ))}
            {resultado.termosRelevantes?.length > 0 && (
              <div className="flex flex-wrap gap-1.5 text-[10.5px] text-slate-600 pt-1">
                Termos que mais pesaram:
                {resultado.termosRelevantes.map(t => (
                  <span key={t} className="px-1.5 py-0.5 rounded-md bg-blue-50 border border-blue-200 text-blue-800 font-medium">{t}</span>
                ))}
              </div>
            )}
          </div>
        ) : (
          <p className="text-xs text-slate-500">Nenhuma palavra conhecida pelo modelo — a IA não arrisca sugestão.</p>
        )
      )}
    </Card>
  )
}

/** Matriz de confusão da validação cruzada: linhas = tipo real, colunas = tipo previsto. */
function MatrizConfusao({ cv }) {
  if (!cv) return null
  const classes = cv.classes
  const maxLinha = cv.matriz.map(l => Math.max(1, l.reduce((a, b) => a + b, 0)))
  return (
    <Card className="p-5">
      <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Matriz de confusão</h3>
      <p className="text-[11px] text-slate-500 mb-3">
        {cv.exemplos} frases testadas sem terem sido vistas no treino. Linha = tipo real · coluna = tipo previsto.
      </p>
      <div className="overflow-x-auto">
        <table className="w-full text-[11px]">
          <thead>
            <tr>
              <th className="text-left font-semibold text-slate-500 pb-2 pr-2">Real \ Previsto</th>
              {classes.map(c => <th key={c} className="font-semibold text-slate-500 pb-2 px-1">{NOMES_CLASSE[c] || c}</th>)}
              <th className="font-semibold text-slate-500 pb-2 px-1">F1</th>
            </tr>
          </thead>
          <tbody>
            {classes.map((real, i) => (
              <tr key={real}>
                <td className="py-1 pr-2 font-semibold text-slate-700">{NOMES_CLASSE[real] || real}</td>
                {classes.map((prev, j) => {
                  const v = cv.matriz[i][j]
                  const intensidade = v / maxLinha[i]
                  const diag = i === j
                  return (
                    <td key={prev} className="p-0.5">
                      <div className={`rounded-md text-center py-1.5 font-mono font-semibold ${diag ? 'text-white' : v ? 'text-red-700' : 'text-slate-300'}`}
                        style={{ background: diag ? `rgba(37,99,235,${0.35 + 0.65 * intensidade})` : v ? `rgba(239,68,68,${0.12 + 0.5 * intensidade})` : '#f8fafc' }}>
                        {v}
                      </div>
                    </td>
                  )
                })}
                <td className="px-1 text-center font-mono text-slate-600">{pct(cv.porClasse[i]?.f1, 0)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

/** Cobertura × acurácia: subir o limiar deixa a IA sugerir menos vezes, mas errar menos. */
function CurvaLimiar({ curva, limiar }) {
  if (!curva.length) return null
  return (
    <Card className="p-5">
      <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Limiar de confiança</h3>
      <p className="text-[11px] text-slate-500 mb-3">
        Cobertura = em quantos chamados a IA faz sugestão · acurácia = quantas dessas sugestões estão certas.
      </p>
      <div className="grid grid-cols-3 md:grid-cols-6 gap-2">
        {curva.map(p => {
          const atual = Math.abs(p.limiar - limiar) < 1e-9
          return (
            <div key={p.limiar} className={`rounded-xl border px-3 py-2 ${atual ? 'border-blue-300 bg-blue-50' : 'border-slate-200 bg-white'}`}>
              <div className="text-[10px] tracking-wide text-slate-500 font-semibold">
                LIMIAR {String(p.limiar.toFixed(1)).replace('.', ',')}{atual && ' · ATUAL'}
              </div>
              <div className="text-[11px] text-slate-600 mt-1">cobertura <b className="text-slate-900">{pct(p.cobertura)}</b></div>
              <div className="text-[11px] text-slate-600">acurácia <b className="text-slate-900">{pct(p.acuracia)}</b></div>
            </div>
          )
        })}
      </div>
    </Card>
  )
}
