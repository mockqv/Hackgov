import { Link } from 'react-router-dom'
import { Plus, FileText, MapPin, ClipboardList } from 'lucide-react'

import { useAuth } from '../../contexts/AuthContext'
import useApi from '../../hooks/useApi'
import { solicitacaoApi } from '../../lib/api'

import Card from '../../components/ui/Card'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'
import StatusBadge from '../../components/ui/StatusBadge'
import { formatDate } from '../../lib/format'

const ACOES = [
  { to: '/nova-solicitacao',     icon: Plus,          title: 'Abrir solicitação',  desc: 'Reporte um problema urbano agora' },
  { to: '/acompanhar',           icon: FileText,      title: 'Acompanhar',         desc: 'Buscar por protocolo'             },
  { to: '/minhas-solicitacoes',  icon: ClipboardList, title: 'Minhas solicitações',desc: 'Histórico do que você abriu'      },
  { to: '/mapa-publico',         icon: MapPin,        title: 'Mapa público',       desc: 'Veja o que acontece na cidade'    },
]

export default function Inicio() {
  const { user } = useAuth()
  const { data: minhas, loading, error, refresh } = useApi(solicitacaoApi.minhas, [])

  const recentes = (minhas || []).slice(0, 4)

  return (
    <div className="max-w-5xl mx-auto p-6 space-y-7">
      {/* Saudação */}
      <header>
        <h1 className="text-xl font-bold text-white">
          Olá, {user?.nome?.split(' ')[0] || 'cidadão'} 👋
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          O que você gostaria de fazer hoje?
        </p>
      </header>

      {/* Atalhos */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {ACOES.map(({ to, icon: Icon, title, desc }) => (
          <Link key={to} to={to}>
            <Card className="p-4 hover:border-blue-600/50 hover:bg-slate-900 transition-colors group">
              <div className="w-9 h-9 rounded-xl bg-blue-600/15 border border-blue-500/20
                              flex items-center justify-center text-blue-300
                              group-hover:bg-blue-600/25 transition-colors">
                <Icon size={16}/>
              </div>
              <div className="mt-3">
                <div className="text-sm font-bold text-white">{title}</div>
                <div className="text-[11px] text-slate-500 mt-0.5">{desc}</div>
              </div>
            </Card>
          </Link>
        ))}
      </div>

      {/* Recentes */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-200">Suas últimas solicitações</h2>
          {!loading && minhas?.length > 0 && (
            <Link to="/minhas-solicitacoes" className="text-xs text-blue-400 hover:text-blue-300 font-semibold">
              Ver todas →
            </Link>
          )}
        </div>

        <Card className="divide-y divide-slate-800/60 overflow-hidden">
          {loading ? (
            <div className="p-4 space-y-3">
              {[1,2,3].map(i => (
                <div key={i} className="flex items-center gap-3">
                  <Skeleton className="h-9 w-9 rounded-xl"/>
                  <div className="flex-1 space-y-1.5">
                    <Skeleton className="h-3 w-1/2"/>
                    <Skeleton className="h-2.5 w-1/3"/>
                  </div>
                  <Skeleton className="h-5 w-16"/>
                </div>
              ))}
            </div>
          ) : error ? (
            <ErrorState onRetry={refresh}/>
          ) : recentes.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="Você ainda não abriu nenhuma solicitação"
              description="Comece reportando um problema urbano. Você acompanha tudo por aqui."
              action={
                <Link to="/nova-solicitacao"
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold
                             bg-blue-600 hover:bg-blue-500 text-white transition-colors">
                  <Plus size={14}/> Abrir solicitação
                </Link>
              }
            />
          ) : (
            recentes.map(s => (
              <Link key={s.id} to={`/acompanhar/${s.protocolo}`}
                className="flex items-center gap-3 p-3.5 hover:bg-slate-800/40 transition-colors">
                <div className="w-9 h-9 rounded-xl bg-slate-800 border border-slate-700/50
                                flex items-center justify-center text-slate-400">
                  <FileText size={15}/>
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-semibold text-white truncate">{s.tipoDescricao}</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">
                    <span className="font-mono">{s.protocolo}</span> · {formatDate(s.dataAbertura)}
                  </div>
                </div>
                <StatusBadge status={s.status}/>
              </Link>
            ))
          )}
        </Card>
      </section>
    </div>
  )
}
