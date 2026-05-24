import { useEffect, useState } from 'react'
import { dashboardApi } from '../services/api'
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip, ResponsiveContainer, Legend
} from 'recharts'
import { RefreshCw, TrendingUp, CheckCircle, Clock, Star } from 'lucide-react'

const COLORS = ['#3B82F6','#EF4444','#F59E0B','#10B981','#8B5CF6','#EC4899']

function KPI({ icon: Icon, label, value, sub, color = 'blue' }) {
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">{label}</span>
        <div className={`w-8 h-8 rounded-lg bg-${color}-600/20 flex items-center justify-center`}>
          <Icon size={15} className={`text-${color}-400`} />
        </div>
      </div>
      <div className={`text-3xl font-black text-${color}-400`}>{value ?? '—'}</div>
      {sub && <div className="text-xs text-gray-500 mt-1">{sub}</div>}
    </div>
  )
}

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-gray-800 border border-gray-700 rounded-xl px-3 py-2 text-xs">
      <div className="text-gray-300 font-semibold mb-1">{label}</div>
      {payload.map((p, i) => (
        <div key={i} className="text-gray-400">{p.name}: <span className="text-white font-bold">{p.value}</span></div>
      ))}
    </div>
  )
}

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    try {
      const res = await dashboardApi.obter()
      setData(res.data)
    } catch { setData(null) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  // Build chart data
  const statusData = data ? [
    { name: 'Abertas',      value: data.abertas,     fill: '#EF4444' },
    { name: 'Em andamento', value: data.emAndamento, fill: '#F59E0B' },
    { name: 'Concluídas',   value: data.concluidas,  fill: '#10B981' },
    { name: 'Canceladas',   value: data.canceladas,  fill: '#6B7280' },
  ].filter(d => d.value > 0) : []

  const tipoData = (data?.countPorTipo || []).map((t, i) => ({
    name: t.tipo.replace('Queimada','').replace('de Árvore','Árvore').replace('de Entulho','Entulho'),
    qtd: t.quantidade,
    fill: COLORS[i % COLORS.length],
  }))

  const tempoData = (data?.tempoMedioPorTipo || []).map(t => ({
    name: t.tipo.split(' ')[0],
    dias: t.mediaDias ? Math.round(t.mediaDias * 10) / 10 : 0,
  }))

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-bold text-white">Dashboard</h1>
          <p className="text-xs text-gray-500 mt-0.5">Indicadores operacionais do sistema</p>
        </div>
        <button onClick={load} disabled={loading}
          className="btn-secondary flex items-center gap-2 text-sm py-2 px-3">
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
          Atualizar
        </button>
      </div>

      {loading && !data ? (
        <div className="text-center py-20 text-gray-500">Carregando métricas...</div>
      ) : !data ? (
        <div className="text-center py-20 text-gray-600 text-sm">
          Sem dados. Verifique a conexão com a API.
        </div>
      ) : (
        <>
          {/* KPIs */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <KPI icon={TrendingUp} label="Total geral"       value={data.totalSolicitacoes} color="blue" />
            <KPI icon={Clock}       label="Abertas"          value={data.abertas}           color="red"  />
            <KPI icon={CheckCircle} label="Taxa de conclusão" value={`${data.taxaConclusao}%`} sub={`${data.concluidas} concluídas`} color="green" />
            <KPI icon={Star}        label="Nota média"       value={data.notaMediaAvaliacao ? `${data.notaMediaAvaliacao}★` : 'N/A'} color="amber" />
          </div>

          {/* Charts row 1 */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

            {/* Pie chart - status */}
            <div className="card p-5">
              <h3 className="text-sm font-semibold text-gray-300 mb-4">Distribuição por Status</h3>
              {statusData.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={statusData} dataKey="value" nameKey="name"
                      cx="50%" cy="50%" outerRadius={80} innerRadius={40}>
                      {statusData.map((entry, i) => (
                        <Cell key={i} fill={entry.fill} stroke="transparent"/>
                      ))}
                    </Pie>
                    <Tooltip content={<CustomTooltip />} />
                    <Legend iconType="circle" iconSize={8}
                      formatter={(v) => <span className="text-xs text-gray-400">{v}</span>} />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-52 flex items-center justify-center text-gray-600 text-sm">
                  Nenhum dado disponível
                </div>
              )}
            </div>

            {/* Bar chart - tipo */}
            <div className="card p-5">
              <h3 className="text-sm font-semibold text-gray-300 mb-4">Solicitações por Tipo</h3>
              {tipoData.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={tipoData} layout="vertical" margin={{ left: 10, right: 20 }}>
                    <XAxis type="number" tick={{ fontSize: 10, fill: '#6B7280' }} axisLine={false} tickLine={false}/>
                    <YAxis type="category" dataKey="name" tick={{ fontSize: 10, fill: '#9CA3AF' }} width={65} axisLine={false} tickLine={false}/>
                    <Tooltip content={<CustomTooltip />} />
                    <Bar dataKey="qtd" name="Quantidade" radius={[0, 6, 6, 0]}>
                      {tipoData.map((entry, i) => (
                        <Cell key={i} fill={entry.fill}/>
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-52 flex items-center justify-center text-gray-600 text-sm">
                  Nenhum dado disponível
                </div>
              )}
            </div>
          </div>

          {/* Bar chart - tempo médio */}
          {tempoData.length > 0 && (
            <div className="card p-5">
              <h3 className="text-sm font-semibold text-gray-300 mb-1">Tempo Médio de Resolução por Tipo (dias)</h3>
              <p className="text-xs text-gray-600 mb-4">Baseado nas solicitações já concluídas</p>
              <ResponsiveContainer width="100%" height={180}>
                <BarChart data={tempoData}>
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#9CA3AF' }} axisLine={false} tickLine={false}/>
                  <YAxis tick={{ fontSize: 10, fill: '#6B7280' }} axisLine={false} tickLine={false}/>
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="dias" name="Dias" fill="#3B82F6" radius={[6,6,0,0]}>
                    {tempoData.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]}/>
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
              <div className="mt-3 p-3 bg-blue-950/30 border border-blue-700/20 rounded-xl">
                <p className="text-xs text-blue-300">
                  💡 <strong>Análise estatística (Fase 2):</strong> Poda de Árvore apresenta maior variabilidade (DP 3,5 dias).
                  Iluminação Queimada é o serviço mais previsível (DP 1,7 dias).
                  Buraco na Via tem P(resolução &gt; SLA) ≈ 38,6%.
                </p>
              </div>
            </div>
          )}

          {/* Summary table */}
          <div className="card p-5">
            <h3 className="text-sm font-semibold text-gray-300 mb-4">Resumo por tipo de serviço</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-800">
                    {['Tipo de serviço', 'Quantidade', 'Participação'].map(h => (
                      <th key={h} className="text-left text-xs text-gray-500 font-semibold uppercase tracking-wider pb-3 pr-4">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800/50">
                  {(data.countPorTipo || []).map((row, i) => {
                    const pct = data.totalSolicitacoes > 0
                      ? Math.round(row.quantidade / data.totalSolicitacoes * 100)
                      : 0
                    return (
                      <tr key={i} className="hover:bg-gray-900/30 transition-colors">
                        <td className="py-3 pr-4 text-gray-200 font-medium">{row.tipo}</td>
                        <td className="py-3 pr-4 text-gray-300 font-mono">{row.quantidade}</td>
                        <td className="py-3">
                          <div className="flex items-center gap-2">
                            <div className="flex-1 h-1.5 bg-gray-800 rounded-full overflow-hidden max-w-20">
                              <div className="h-full rounded-full" style={{ width: `${pct}%`, background: COLORS[i % COLORS.length] }}/>
                            </div>
                            <span className="text-xs text-gray-500">{pct}%</span>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
