import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, RefreshCw, Search } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { solicitacaoApi } from '../../lib/api'
import { useAuth } from '../../contexts/AuthContext'

import CityMap from '../../components/map/CityMap'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'
import StatusBadge, { STATUS_DOT } from '../../components/ui/StatusBadge'
import { Input } from '../../components/ui/Input'
import { formatDate } from '../../lib/format'

const ABERTOS = ['RECEBIDO','EM_ANALISE','AGENDADO','EM_EXECUCAO']

export default function MapaPublico() {
  const { isAuthenticated, user } = useAuth()
  const { data, loading, error, refresh } = useApi(solicitacaoApi.mapa, [])
  const items = data || []

  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState(null)

  const counts = items.reduce((acc, i) => {
    if (i.status === 'RECEBIDO')               acc.abertas++
    else if (ABERTOS.includes(i.status))       acc.andamento++
    else if (i.status === 'CONCLUIDO')         acc.concluidas++
    return acc
  }, { abertas: 0, andamento: 0, concluidas: 0 })

  const lista = (search
    ? items.filter(i =>
        i.protocolo?.toLowerCase().includes(search.toLowerCase()) ||
        i.tipoDescricao?.toLowerCase().includes(search.toLowerCase()) ||
        i.nomeBairro?.toLowerCase().includes(search.toLowerCase())
      )
    : items
  ).slice(0, 50)

  return (
    <div className="flex-1 flex flex-col">
      {/* Header */}
      <div className="h-14 flex items-center justify-between px-5 border-b border-slate-800 flex-shrink-0">
        <div>
          <h1 className="text-sm font-bold text-white">Mapa público</h1>
          <p className="text-[11px] text-slate-500">{items.length} solicitações no mapa</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={refresh}>
            <RefreshCw size={12} className={loading ? 'animate-spin' : ''}/> Atualizar
          </Button>
          {isAuthenticated && user?.perfil === 'CIDADAO' && (
            <Link to="/nova-solicitacao">
              <Button size="sm"><Plus size={12}/> Nova solicitação</Button>
            </Link>
          )}
          {!isAuthenticated && (
            <Link to="/login">
              <Button size="sm">Entrar</Button>
            </Link>
          )}
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden">
        {/* Mapa */}
        <div className="flex-1 relative p-4">
          {/* Stats flutuante */}
          <div className="absolute top-6 left-6 z-[400] flex gap-2">
            {[
              { val: counts.abertas,    label: 'Abertas',      cls: 'text-red-400'   },
              { val: counts.andamento,  label: 'Em andamento', cls: 'text-amber-400' },
              { val: counts.concluidas, label: 'Concluídas',   cls: 'text-green-400' },
            ].map(s => (
              <Card key={s.label} className="px-3 py-2 text-center">
                <div className={`text-base font-black ${s.cls}`}>{s.val}</div>
                <div className="text-[9px] text-slate-500 uppercase tracking-wider">{s.label}</div>
              </Card>
            ))}
          </div>

          {/* Legenda */}
          <div className="absolute bottom-6 left-6 z-[400]">
            <Card className="p-3 space-y-1.5">
              {Object.entries(STATUS_DOT).filter(([k]) => !['CANCELADO'].includes(k)).map(([k, c]) => (
                <div key={k} className="flex items-center gap-2 text-[10.5px] text-slate-300">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ background: c }}/>
                  {k.replace('_', ' ').toLowerCase()}
                </div>
              ))}
            </Card>
          </div>

          {loading
            ? <Skeleton className="w-full h-full rounded-2xl"/>
            : error
              ? <ErrorState onRetry={refresh}/>
              : <CityMap items={items} selectedId={selected?.id} onSelect={setSelected}/>
          }
        </div>

        {/* Side panel */}
        <aside className="w-80 border-l border-slate-800 flex flex-col overflow-hidden">
          <div className="p-3.5 border-b border-slate-800">
            <Input
              placeholder="Buscar por protocolo, tipo, bairro..."
              leftIcon={<Search size={14}/>}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {selected ? (
            <div className="p-4 space-y-3">
              <div className="flex items-start justify-between">
                <span className="font-mono text-[11px] text-slate-500">{selected.protocolo}</span>
                <button onClick={() => setSelected(null)}
                  className="text-slate-500 hover:text-slate-200 text-lg leading-none">×</button>
              </div>
              <div>
                <div className="text-sm font-bold text-white">{selected.tipoDescricao}</div>
                <div className="text-[11px] text-slate-400 mt-1">
                  {selected.logradouro} {selected.logradouro && '— '}{selected.nomeBairro}
                </div>
              </div>
              <StatusBadge status={selected.status}/>
              <div className="text-[11px] text-slate-500">
                Aberto em {formatDate(selected.dataAbertura)}
              </div>
              <Link to={`/acompanhar/${selected.protocolo}`}>
                <Button size="sm" className="w-full">Ver detalhes</Button>
              </Link>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto divide-y divide-slate-800/60">
              {loading ? (
                <div className="p-4 space-y-3">
                  {[1,2,3].map(i => <Skeleton key={i} className="h-12"/>)}
                </div>
              ) : lista.length === 0 ? (
                <EmptyState
                  title={search ? 'Nada encontrado' : 'Sem solicitações ainda'}
                  description={search ? 'Tente outros termos.' : 'Quando alguém abrir um chamado ele aparece aqui.'}
                />
              ) : (
                lista.map(item => (
                  <button key={item.id} onClick={() => setSelected(item)}
                    className="w-full text-left p-3.5 hover:bg-slate-800/40 transition-colors">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <div className="font-mono text-[9.5px] text-slate-500">{item.protocolo}</div>
                        <div className="text-xs font-semibold text-slate-200 mt-0.5 truncate">{item.tipoDescricao}</div>
                        <div className="text-[10px] text-slate-500 mt-0.5 truncate">{item.nomeBairro}</div>
                      </div>
                      <span className="w-2 h-2 rounded-full flex-shrink-0 mt-1.5"
                        style={{ background: STATUS_DOT[item.status] }}/>
                    </div>
                  </button>
                ))
              )}
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}
