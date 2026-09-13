import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { TrendingUp, CheckCircle, Clock, Star, RefreshCw, ShieldCheck, MapPin } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { dashboardApi } from '../../lib/api'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'

const COLORS = ['#2563eb','#ef4444','#f59e0b','#10b981','#8b5cf6','#ec4899']

function Kpi({ icon: Icon, label, value, hint, accent = 'blue' }) {
  const accents = {
    blue:  'text-blue-600    bg-blue-50    border-blue-200',
    red:   'text-red-600     bg-red-50     border-red-200',
    green: 'text-emerald-600 bg-emerald-50 border-emerald-200',
    amber: 'text-amber-600   bg-amber-50   border-amber-200',
  }
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{label}</span>
        <div className={`w-9 h-9 rounded-xl border flex items-center justify-center ${accents[accent]}`}>
          <Icon size={16}/>
        </div>
      </div>
      <div className="text-3xl font-black font-display text-slate-900">{value ?? '—'}</div>
      {hint && <div className="text-[11px] text-slate-500 mt-1">{hint}</div>}
    </Card>
  )
}

const ChartTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white border border-slate-200 rounded-xl px-3 py-2 text-xs shadow-lg">
      {label && <div className="text-slate-600 font-semibold mb-1">{label}</div>}
      {payload.map((p, i) => (
        <div key={i} className="text-slate-500">
          {p.name}: <span className="text-slate-900 font-bold">{p.value}</span>
        </div>
      ))}
    </div>
  )
}

export default function Dashboard() {
  const { data, loading, error, refresh } = useApi(dashboardApi.obter, [])

  const statusData = data ? [
    { name: 'Abertas',      value: data.abertas,     fill: '#ef4444' },
    { name: 'Em andamento', value: data.emAndamento, fill: '#f59e0b' },
    { name: 'Concluídas',   value: data.concluidas,  fill: '#10b981' },
    { name: 'Canceladas',   value: data.canceladas,  fill: '#94a3b8' },
  ].filter(d => d.value > 0) : []

  const tipoData = (data?.countPorTipo || []).map((t, i) => ({
    name: t.tipo, qtd: t.quantidade, fill: COLORS[i % COLORS.length],
  }))

  const tempoData = (data?.tempoMedioPorTipo || []).map(t => ({
    name: t.tipo.split(' ')[0],
    dias: t.mediaDias ? Math.round(t.mediaDias * 10) / 10 : 0,
  }))

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-bold font-display text-slate-900">Dashboard</h1>
          <p className="text-xs text-slate-500 mt-0.5">Indicadores operacionais do sistema</p>
        </div>
        <Button variant="secondary" size="sm" onClick={refresh}>
          <RefreshCw size={12} className={loading ? 'animate-spin' : ''}/> Atualizar
        </Button>
      </header>

      {loading && !data ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[1,2,3,4].map(i => <Skeleton key={i} className="h-28 rounded-2xl"/>)}
        </div>
      ) : error ? (
        <ErrorState onRetry={refresh}/>
      ) : !data ? (
        <EmptyState title="Sem dados disponíveis"/>
      ) : (
        <>
          {/* KPIs */}
          <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
            <Kpi icon={TrendingUp} label="Total geral" value={data.totalSolicitacoes}/>
            <Kpi icon={Clock}       label="Abertas"     value={data.abertas} accent="red"/>
            <Kpi icon={CheckCircle} label="Taxa conclusão"
                 value={data.taxaConclusao != null ? `${data.taxaConclusao}%` : '—'}
                 hint={`${data.concluidas} concluídas`} accent="green"/>
            <Kpi icon={ShieldCheck} label="SLA cumprido"
                 value={data.slaCumpridoPercentual != null ? `${data.slaCumpridoPercentual}%` : 'N/A'}
                 hint="Concluídas dentro do prazo" accent={
                   data.slaCumpridoPercentual == null ? 'blue'
                     : data.slaCumpridoPercentual >= 80 ? 'green'
                     : data.slaCumpridoPercentual >= 50 ? 'amber' : 'red'
                 }/>
            <Kpi icon={Star}        label="Nota média"
                 value={data.notaMediaAvaliacao ? `${data.notaMediaAvaliacao}★` : 'N/A'} accent="amber"/>
          </div>

          {/* Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">Distribuição por status</h3>
              {statusData.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={statusData} dataKey="value" nameKey="name"
                         cx="50%" cy="50%" outerRadius={80} innerRadius={40} stroke="transparent">
                      {statusData.map((e, i) => <Cell key={i} fill={e.fill}/>)}
                    </Pie>
                    <Tooltip content={<ChartTooltip/>}/>
                    <Legend iconType="circle" iconSize={8}
                      formatter={(v) => <span className="text-xs text-slate-500">{v}</span>}/>
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState title="Sem dados de status"/>
              )}
            </Card>

            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">Solicitações por tipo</h3>
              {tipoData.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={tipoData} layout="vertical" margin={{ left: 10, right: 20 }}>
                    <XAxis type="number" tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} tickLine={false}/>
                    <YAxis type="category" dataKey="name" tick={{ fontSize: 10, fill: '#475569' }} width={80} axisLine={false} tickLine={false}/>
                    <Tooltip content={<ChartTooltip/>}/>
                    <Bar dataKey="qtd" name="Quantidade" radius={[0, 6, 6, 0]}>
                      {tipoData.map((e, i) => <Cell key={i} fill={e.fill}/>)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState title="Sem dados por tipo"/>
              )}
            </Card>
          </div>

          {tempoData.length > 0 && (
            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Tempo médio de resolução (dias)</h3>
              <p className="text-[11px] text-slate-500 mb-4">Baseado nas solicitações já concluídas.</p>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={tempoData}>
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#475569' }} axisLine={false} tickLine={false}/>
                  <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} tickLine={false}/>
                  <Tooltip content={<ChartTooltip/>}/>
                  <Bar dataKey="dias" name="Dias" radius={[6,6,0,0]}>
                    {tempoData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]}/>)}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Card>
          )}

          {data.countPorTipo?.length > 0 && (
            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">Resumo por tipo</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left">
                      {['Tipo','Quantidade','Participação'].map(h => (
                        <th key={h} className="pb-3 pr-4 text-[10px] text-slate-500 font-semibold uppercase tracking-wider">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {data.countPorTipo.map((row, i) => {
                      const pct = data.totalSolicitacoes > 0
                        ? Math.round(row.quantidade / data.totalSolicitacoes * 100) : 0
                      return (
                        <tr key={i} className="hover:bg-slate-50 transition-colors">
                          <td className="py-3 pr-4 text-slate-700 font-medium">{row.tipo}</td>
                          <td className="py-3 pr-4 text-slate-600 font-mono">{row.quantidade}</td>
                          <td className="py-3">
                            <div className="flex items-center gap-2">
                              <div className="flex-1 h-1.5 bg-slate-100 rounded-full overflow-hidden max-w-32">
                                <div className="h-full rounded-full"
                                  style={{ width: `${pct}%`, background: COLORS[i % COLORS.length] }}/>
                              </div>
                              <span className="text-[11px] text-slate-500 w-8 text-right">{pct}%</span>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </Card>
          )}

          {data.topBairros?.length > 0 && (
            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                <MapPin size={12}/> Top 5 bairros por volume
              </h3>
              <p className="text-[11px] text-slate-500 mb-4">
                Onde concentrar mutirões e reforço de equipe de campo.
              </p>
              <div className="space-y-2.5">
                {data.topBairros.map((b, i) => {
                  const max = data.topBairros[0].quantidade || 1
                  const pct = Math.round((b.quantidade / max) * 100)
                  return (
                    <div key={i} className="flex items-center gap-3">
                      <span className="text-[11px] text-slate-500 w-5 text-right font-mono">{i + 1}</span>
                      <span className="text-xs text-slate-700 w-32 truncate">{b.bairro}</span>
                      <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div className="h-full rounded-full" style={{ width: `${pct}%`, background: COLORS[i % COLORS.length] }}/>
                      </div>
                      <span className="text-[11px] text-slate-500 w-6 text-right font-mono">{b.quantidade}</span>
                    </div>
                  )
                })}
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  )
}
