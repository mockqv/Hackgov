import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { TrendingUp, CheckCircle, Clock, Star, RefreshCw } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { dashboardApi } from '../../lib/api'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'

const COLORS = ['#3b82f6','#ef4444','#f59e0b','#10b981','#8b5cf6','#ec4899']

function Kpi({ icon: Icon, label, value, hint, accent = 'blue' }) {
  const accents = {
    blue:  'text-blue-300  bg-blue-500/10  border-blue-500/20',
    red:   'text-red-300   bg-red-500/10   border-red-500/20',
    green: 'text-green-300 bg-green-500/10 border-green-500/20',
    amber: 'text-amber-300 bg-amber-500/10 border-amber-500/20',
  }
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{label}</span>
        <div className={`w-9 h-9 rounded-xl border flex items-center justify-center ${accents[accent]}`}>
          <Icon size={16}/>
        </div>
      </div>
      <div className="text-3xl font-black text-white">{value ?? '—'}</div>
      {hint && <div className="text-[11px] text-slate-500 mt-1">{hint}</div>}
    </Card>
  )
}

const ChartTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs shadow-lg">
      {label && <div className="text-slate-300 font-semibold mb-1">{label}</div>}
      {payload.map((p, i) => (
        <div key={i} className="text-slate-400">
          {p.name}: <span className="text-white font-bold">{p.value}</span>
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
    { name: 'Canceladas',   value: data.canceladas,  fill: '#64748b' },
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
          <h1 className="text-lg font-bold text-white">Dashboard</h1>
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
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <Kpi icon={TrendingUp} label="Total geral" value={data.totalSolicitacoes}/>
            <Kpi icon={Clock}       label="Abertas"     value={data.abertas} accent="red"/>
            <Kpi icon={CheckCircle} label="Taxa conclusão"
                 value={data.taxaConclusao != null ? `${data.taxaConclusao}%` : '—'}
                 hint={`${data.concluidas} concluídas`} accent="green"/>
            <Kpi icon={Star}        label="Nota média"
                 value={data.notaMediaAvaliacao ? `${data.notaMediaAvaliacao}★` : 'N/A'} accent="amber"/>
          </div>

          {/* Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4">Distribuição por status</h3>
              {statusData.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={statusData} dataKey="value" nameKey="name"
                         cx="50%" cy="50%" outerRadius={80} innerRadius={40} stroke="transparent">
                      {statusData.map((e, i) => <Cell key={i} fill={e.fill}/>)}
                    </Pie>
                    <Tooltip content={<ChartTooltip/>}/>
                    <Legend iconType="circle" iconSize={8}
                      formatter={(v) => <span className="text-xs text-slate-400">{v}</span>}/>
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState title="Sem dados de status"/>
              )}
            </Card>

            <Card className="p-5">
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4">Solicitações por tipo</h3>
              {tipoData.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={tipoData} layout="vertical" margin={{ left: 10, right: 20 }}>
                    <XAxis type="number" tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false}/>
                    <YAxis type="category" dataKey="name" tick={{ fontSize: 10, fill: '#94a3b8' }} width={80} axisLine={false} tickLine={false}/>
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
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">Tempo médio de resolução (dias)</h3>
              <p className="text-[11px] text-slate-500 mb-4">Baseado nas solicitações já concluídas.</p>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={tempoData}>
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false}/>
                  <YAxis tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false}/>
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
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4">Resumo por tipo</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-800 text-left">
                      {['Tipo','Quantidade','Participação'].map(h => (
                        <th key={h} className="pb-3 pr-4 text-[10px] text-slate-500 font-semibold uppercase tracking-wider">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {data.countPorTipo.map((row, i) => {
                      const pct = data.totalSolicitacoes > 0
                        ? Math.round(row.quantidade / data.totalSolicitacoes * 100) : 0
                      return (
                        <tr key={i} className="hover:bg-slate-900/40 transition-colors">
                          <td className="py-3 pr-4 text-slate-200 font-medium">{row.tipo}</td>
                          <td className="py-3 pr-4 text-slate-300 font-mono">{row.quantidade}</td>
                          <td className="py-3">
                            <div className="flex items-center gap-2">
                              <div className="flex-1 h-1.5 bg-slate-800 rounded-full overflow-hidden max-w-32">
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
        </>
      )}
    </div>
  )
}
