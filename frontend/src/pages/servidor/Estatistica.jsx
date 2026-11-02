import { LineChart as IconeGrafico, RefreshCw, Lightbulb, Sigma } from 'lucide-react'
import {
  CartesianGrid, ComposedChart, Line, ResponsiveContainer, Scatter, Tooltip, XAxis, YAxis,
} from 'recharts'

import useApi from '../../hooks/useApi'
import { analiseApi } from '../../lib/api'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'

const num = (v, casas = 2) => (v == null || Number.isNaN(v) ? '—' : v.toFixed(casas).replace('.', ',').replace('-', '−'))
const valorP = (p) => (p < 0.001 ? '< 0,001' : num(p, 3))

// as notas são inteiras (1 a 5): uma dispersão vertical pequena e determinística evita
// que centenas de pontos fiquem empilhados no mesmo lugar (só no desenho; o cálculo usa o valor real)
function comDispersao(pontos, inteiros) {
  if (!inteiros) return pontos
  return pontos.map((p, i) => ({ ...p, y: p.y + (((i * 7919) % 100) / 100 - 0.5) * 0.35 }))
}

function Indicador({ rotulo, valor, destaque }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
      <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{rotulo}</div>
      <div className={`text-lg font-black font-display ${destaque || 'text-slate-900'}`}>{valor}</div>
    </div>
  )
}

function DicaPonto({ active, payload }) {
  if (!active || !payload?.length) return null
  const p = payload[0].payload
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-[11px] shadow-sm">
      {p.rotulo && <div className="font-semibold text-slate-800">{p.rotulo}</div>}
      <div className="text-slate-600">x = {num(p.x, 1)} · y = {num(p.yReal ?? p.y, 2)}</div>
    </div>
  )
}

function Analise({ a }) {
  const inteiros = a.id === 'atraso-nota'
  const pontos = comDispersao(a.pontos.map(p => ({ ...p, yReal: p.y })), inteiros)
  const xs = a.pontos.map(p => p.x).concat((a.projecao || []).map((_, i) => a.pontos.length + i + 1))
  const minX = Math.min(...xs), maxX = Math.max(...xs)
  const reta = [{ x: minX, y: a.intercepto + a.inclinacao * minX }, { x: maxX, y: a.intercepto + a.inclinacao * maxX }]
  const projecao = (a.projecao || []).map((p, i) => ({ x: a.pontos.length + i + 1, y: p.previsto, rotulo: `projeção semana de ${p.semana}` }))

  return (
    <Card className="p-5 space-y-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-bold text-slate-900">{a.titulo}</h3>
          <p className="text-xs text-slate-500 mt-0.5">{a.pergunta}</p>
        </div>
        <span className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide ${
          a.significativa ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-600 border border-slate-200'}`}>
          {a.significativa ? 'significativa (p < 0,05)' : 'não significativa'}
        </span>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
        <Indicador rotulo="r de Pearson" valor={num(a.r)} destaque={a.r < 0 ? 'text-rose-600' : 'text-blue-700'} />
        <Indicador rotulo="R²" valor={`${num(a.r2 * 100, 0)}%`} />
        <Indicador rotulo="valor-p" valor={valorP(a.valorP)} />
        <Indicador rotulo="n" valor={a.n} />
      </div>

      <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">eixo y: {a.eixoY}</div>
      <div className="h-64 -mt-2">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart margin={{ top: 8, right: 12, bottom: 22, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
            <XAxis type="number" dataKey="x" domain={['auto', 'auto']} tick={{ fontSize: 10 }}
                   label={{ value: a.eixoX, position: 'insideBottom', offset: -12, fontSize: 10, fill: '#64748b' }} />
            <YAxis type="number" dataKey="y" tick={{ fontSize: 10 }} width={42}
                   domain={inteiros ? [0.5, 5.5] : ['auto', 'auto']} ticks={inteiros ? [1, 2, 3, 4, 5] : undefined}
                   allowDataOverflow={inteiros} />
            <Tooltip content={<DicaPonto />} />
            <Scatter data={pontos} fill="#2563eb" fillOpacity={inteiros ? 0.25 : 0.75} r={inteiros ? 2 : 4} isAnimationActive={false} />
            {projecao.length > 0 && <Scatter data={projecao} fill="#f59e0b" shape="diamond" isAnimationActive={false} />}
            <Line data={reta} dataKey="y" type="linear" stroke="#0f172a" strokeWidth={2} dot={false} isAnimationActive={false} />
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      {a.projecao?.length > 0 && (
        <table className="w-full text-xs">
          <thead>
            <tr className="text-left text-[10px] uppercase tracking-wider text-slate-500">
              <th className="py-1">Semana</th><th>Previsto</th><th>Intervalo de 95%</th>
            </tr>
          </thead>
          <tbody>
            {a.projecao.map(p => (
              <tr key={p.semana} className="border-t border-slate-100">
                <td className="py-1 font-medium text-slate-700">{p.semana}</td>
                <td className="font-bold text-amber-600">{num(p.previsto, 0)}</td>
                <td className="text-slate-600">{num(p.minimo, 0)} a {num(p.maximo, 0)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div className="space-y-2 text-xs leading-relaxed">
        <p className="flex gap-2 text-slate-600"><Sigma size={14} className="shrink-0 mt-0.5 text-slate-400" />{a.leitura}</p>
        <p className="flex gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-amber-900">
          <Lightbulb size={14} className="shrink-0 mt-0.5" /><span><b>Decisão: </b>{a.decisao}</span>
        </p>
      </div>
    </Card>
  )
}

/**
 * Tela "Estatística" (Fase 7, Parte 3) — servidor e gestor.
 * Correlação de Pearson e regressão linear calculadas no back-end (EstatisticaService),
 * com teste de significância e a leitura para decisão de cada análise.
 */
export default function Estatistica() {
  const rel = useApi(analiseApi.estatistica, [])
  const d = rel.data

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-bold font-display text-slate-900 flex items-center gap-2"><IconeGrafico size={18} /> Estatística</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Correlação e regressão sobre os chamados{d ? ` · ${d.chamados} chamados até ${d.referencia?.split('-').reverse().join('/')}` : ''}
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={rel.refresh}>
          <RefreshCw size={12} className={rel.loading ? 'animate-spin' : ''} /> Atualizar
        </Button>
      </header>

      {rel.loading && !d ? (
        <div className="grid lg:grid-cols-2 gap-4">{[1, 2, 3, 4].map(i => <Skeleton key={i} className="h-96 rounded-2xl" />)}</div>
      ) : rel.error ? (
        <ErrorState onRetry={rel.refresh} />
      ) : !d?.analises?.length ? (
        <EmptyState title="Dados insuficientes" description="As análises aparecem quando houver chamados concluídos e avaliados." />
      ) : (
        <>
          <Card className="p-4 text-xs text-slate-600 leading-relaxed">
            <b className="text-slate-800">Como ler:</b> r vai de −1 a 1 (sinal = sentido, módulo = força da relação linear).
            R² é a fração da variação de y explicada por x. O valor-p testa se a inclinação pode ser zero: abaixo de 0,05,
            a relação dificilmente é acaso da amostra. Correlação não prova causa — a decisão sugerida combina o número
            com o conhecimento da operação.
          </Card>
          <div className="grid lg:grid-cols-2 gap-4">
            {d.analises.map(a => <Analise key={a.id} a={a} />)}
          </div>
        </>
      )}
    </div>
  )
}
