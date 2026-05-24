import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { solicitacaoApi } from '../services/api'
import { Plus, RefreshCw, MapPin } from 'lucide-react'

const STATUS_COLOR = {
  RECEBIDO:    { bg: '#DC2626', label: 'Aberto'        },
  EM_ANALISE:  { bg: '#D97706', label: 'Em análise'    },
  AGENDADO:    { bg: '#D97706', label: 'Agendado'      },
  EM_EXECUCAO: { bg: '#2563EB', label: 'Em execução'   },
  CONCLUIDO:   { bg: '#16A34A', label: 'Concluído'     },
  CANCELADO:   { bg: '#6B7280', label: 'Cancelado'     },
}

export default function Mapa() {
  const [items, setItems] = useState([])
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const navigate = useNavigate()

  async function load() {
    setLoading(true)
    try {
      const res = await solicitacaoApi.mapa()
      setItems(res.data || [])
    } catch { setItems([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const counts = items.reduce((acc, i) => {
    const s = i.status
    if (['RECEBIDO'].includes(s))                  acc.abertas++
    if (['EM_ANALISE','AGENDADO','EM_EXECUCAO'].includes(s)) acc.andamento++
    if (s === 'CONCLUIDO')                         acc.concluidas++
    return acc
  }, { abertas: 0, andamento: 0, concluidas: 0 })

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="h-16 flex items-center justify-between px-6 border-b border-gray-800 flex-shrink-0">
        <div>
          <h1 className="text-base font-bold text-white">Mapa Público</h1>
          <p className="text-xs text-gray-500">{items.length} solicitações registradas</p>
        </div>
        <div className="flex gap-2">
          <button onClick={load} className="btn-secondary flex items-center gap-2 text-sm py-2 px-3">
            <RefreshCw size={13} />
            Atualizar
          </button>
          <button onClick={() => navigate('/nova')} className="btn-primary flex items-center gap-2 text-sm py-2 px-3">
            <Plus size={13} />
            Nova solicitação
          </button>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Map area — visual SVG map */}
        <div className="flex-1 bg-gray-950 relative overflow-hidden">
          {/* Stats overlay */}
          <div className="absolute top-4 left-4 z-10 flex gap-2">
            {[
              { label: 'Abertas',       val: counts.abertas,    color: 'red'  },
              { label: 'Em andamento',  val: counts.andamento,  color: 'amber'},
              { label: 'Concluídas',    val: counts.concluidas, color: 'green'},
            ].map(({ label, val, color }) => (
              <div key={label} className="card px-3 py-2 text-center">
                <div className={`text-lg font-bold text-${color}-400`}>{val}</div>
                <div className="text-[10px] text-gray-500">{label}</div>
              </div>
            ))}
          </div>

          {/* SVG map */}
          <svg className="w-full h-full" viewBox="0 0 800 520" xmlns="http://www.w3.org/2000/svg">
            <rect width="800" height="520" fill="#0A0F1E"/>
            {/* Grid */}
            {[80,160,240,320,400,480,560,640,720].map(x =>
              <line key={x} x1={x} y1="0" x2={x} y2="520" stroke="#111827" strokeWidth="1"/>)}
            {[80,160,240,320,400,480].map(y =>
              <line key={y} x1="0" y1={y} x2="800" y2={y} stroke="#111827" strokeWidth="1"/>)}
            {/* Roads */}
            <line x1="0" y1="160" x2="800" y2="160" stroke="#1F2D45" strokeWidth="14"/>
            <line x1="0" y1="320" x2="800" y2="320" stroke="#1F2D45" strokeWidth="10"/>
            <line x1="200" y1="0" x2="200" y2="520" stroke="#1F2D45" strokeWidth="14"/>
            <line x1="560" y1="0" x2="560" y2="520" stroke="#1F2D45" strokeWidth="10"/>
            {/* Road markings */}
            <line x1="0" y1="160" x2="800" y2="160" stroke="#1E3A5F" strokeWidth="1" strokeDasharray="20,14"/>
            <line x1="200" y1="0" x2="200" y2="520" stroke="#1E3A5F" strokeWidth="1" strokeDasharray="20,14"/>
            {/* Blocks */}
            {[[20,20,160,130],[220,20,320,130],[360,20,200,130],[580,20,200,130],
              [20,180,160,120],[220,180,120,120],[360,180,180,120],[580,180,200,120],
              [20,350,340,140],[380,350,160,140],[560,350,220,140]].map(([x,y,w,h],i)=>
              <rect key={i} x={x} y={y} width={w} height={h} fill="#0F1A2A" rx="3"/>)}
            {/* Dynamic pins from API */}
            {items.slice(0, 20).map((item, i) => {
              const x = 100 + (i % 7) * 100
              const y = 80 + Math.floor(i / 7) * 140
              const col = STATUS_COLOR[item.status]?.bg || '#6B7280'
              const isSelected = selected?.id === item.id
              return (
                <g key={item.id} onClick={() => setSelected(item)} style={{cursor:'pointer'}}>
                  {isSelected && <circle cx={x} cy={y} r="22" fill={col} opacity="0.25"/>}
                  <circle cx={x} cy={y} r="12" fill={col} stroke="rgba(255,255,255,0.3)" strokeWidth="2"/>
                  <text x={x} y={y+4} textAnchor="middle" fontSize="9" fill="white" fontWeight="800">
                    {item.status[0]}
                  </text>
                  <polygon points={`${x},${y+12} ${x-5},${y+20} ${x+5},${y+20}`} fill={col}/>
                </g>
              )
            })}
            {/* Empty state pins for demo when no API */}
            {items.length === 0 && [
              [120, 100, '#DC2626'], [340, 90, '#DC2626'], [480, 130, '#D97706'],
              [620, 100, '#16A34A'], [200, 250, '#D97706'], [400, 260, '#DC2626'],
              [580, 250, '#16A34A'], [100, 400, '#16A34A'], [350, 410, '#DC2626'],
            ].map(([x, y, color], i) => (
              <g key={i}>
                <circle cx={x} cy={y} r="12" fill={color} stroke="rgba(255,255,255,0.3)" strokeWidth="2"/>
                <text x={x} y={y+4} textAnchor="middle" fontSize="9" fill="white" fontWeight="800">
                  {color === '#DC2626' ? 'A' : color === '#D97706' ? 'E' : 'C'}
                </text>
                <polygon points={`${x},${y+12} ${x-5},${y+20} ${x+5},${y+20}`} fill={color}/>
              </g>
            ))}
          </svg>

          {/* Legend */}
          <div className="absolute bottom-4 left-4 card p-3 space-y-1.5">
            {Object.entries(STATUS_COLOR).filter(([k]) => !['AGENDADO','EM_EXECUCAO','CANCELADO'].includes(k))
              .map(([, { bg, label }]) => (
                <div key={label} className="flex items-center gap-2 text-[11px] text-gray-300">
                  <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{background:bg}}/>
                  {label}
                </div>
              ))}
          </div>
        </div>

        {/* Right panel */}
        <div className="w-72 border-l border-gray-800 flex flex-col overflow-hidden">
          <div className="p-4 border-b border-gray-800">
            <input className="input text-sm" placeholder="Buscar por protocolo..."
              value={search} onChange={e => setSearch(e.target.value)} />
          </div>

          {selected ? (
            <div className="p-4 space-y-3">
              <div className="flex items-start justify-between">
                <span className="font-mono text-xs text-gray-400">{selected.protocolo}</span>
                <button onClick={() => setSelected(null)} className="text-gray-600 hover:text-gray-300 text-lg leading-none">×</button>
              </div>
              <div>
                <div className="text-sm font-bold text-white">{selected.tipoDescricao}</div>
                <div className="text-xs text-gray-400 mt-1">{selected.logradouro} — {selected.nomeBairro}</div>
              </div>
              <div className="flex items-center gap-2">
                <span className="badge text-[10px] font-bold px-2 py-1 rounded-lg"
                  style={{
                    background: STATUS_COLOR[selected.status]?.bg + '22',
                    color: STATUS_COLOR[selected.status]?.bg,
                    border: `1px solid ${STATUS_COLOR[selected.status]?.bg}44`,
                  }}>
                  {STATUS_COLOR[selected.status]?.label || selected.status}
                </span>
              </div>
              <div className="text-xs text-gray-500">
                Aberto em {new Date(selected.dataAbertura).toLocaleDateString('pt-BR')}
              </div>
              <button onClick={() => navigate(`/acompanhar?proto=${selected.protocolo}`)}
                className="btn-primary w-full text-sm py-2">
                Ver detalhes
              </button>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto divide-y divide-gray-800/50">
              {loading ? (
                <div className="p-4 text-center text-gray-500 text-sm">Carregando...</div>
              ) : (
                (search
                  ? items.filter(i => i.protocolo?.toLowerCase().includes(search.toLowerCase()))
                  : items
                ).slice(0, 30).map(item => (
                  <button key={item.id} onClick={() => setSelected(item)}
                    className="w-full text-left p-3 hover:bg-gray-900/50 transition-colors">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="font-mono text-[10px] text-gray-500">{item.protocolo}</div>
                        <div className="text-xs font-semibold text-gray-200 mt-0.5">{item.tipoDescricao}</div>
                        <div className="text-[10px] text-gray-500 mt-0.5">{item.nomeBairro}</div>
                      </div>
                      <span className="w-2 h-2 rounded-full flex-shrink-0 mt-1"
                        style={{background: STATUS_COLOR[item.status]?.bg}}/>
                    </div>
                  </button>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
