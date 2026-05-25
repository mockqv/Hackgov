import { Link } from 'react-router-dom'
import { Plus, FileText } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { solicitacaoApi } from '../../lib/api'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'
import StatusBadge from '../../components/ui/StatusBadge'
import { formatDate } from '../../lib/format'

export default function MinhasSolicitacoes() {
  const { data, loading, error, refresh } = useApi(solicitacaoApi.minhas, [])
  const items = data || []

  return (
    <div className="max-w-3xl mx-auto p-6 space-y-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-bold text-white">Minhas solicitações</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {items.length} {items.length === 1 ? 'solicitação' : 'solicitações'}
          </p>
        </div>
        <Link to="/nova-solicitacao">
          <Button size="sm"><Plus size={13}/> Nova</Button>
        </Link>
      </header>

      {loading ? (
        <Card className="p-4 space-y-3">
          {[1,2,3,4].map(i => (
            <div key={i} className="flex items-center gap-3">
              <Skeleton className="h-9 w-9 rounded-xl"/>
              <div className="flex-1 space-y-1.5">
                <Skeleton className="h-3 w-1/2"/>
                <Skeleton className="h-2.5 w-1/3"/>
              </div>
              <Skeleton className="h-5 w-16"/>
            </div>
          ))}
        </Card>
      ) : error ? (
        <ErrorState onRetry={refresh}/>
      ) : items.length === 0 ? (
        <Card className="overflow-hidden">
          <EmptyState
            icon={FileText}
            title="Nenhuma solicitação ainda"
            description="Suas solicitações abertas aparecem aqui em ordem cronológica."
            action={
              <Link to="/nova-solicitacao">
                <Button size="sm"><Plus size={13}/> Abrir a primeira</Button>
              </Link>
            }
          />
        </Card>
      ) : (
        <Card className="divide-y divide-slate-800/60 overflow-hidden">
          {items.map(s => (
            <Link key={s.id} to={`/acompanhar/${s.protocolo}`}
              className="flex items-center gap-3 p-3.5 hover:bg-slate-800/40 transition-colors">
              <div className="w-9 h-9 rounded-xl bg-slate-800 border border-slate-700/50 flex items-center justify-center text-slate-400">
                <FileText size={15}/>
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-semibold text-white truncate">{s.tipoDescricao}</div>
                <div className="text-[11px] text-slate-500 mt-0.5">
                  <span className="font-mono">{s.protocolo}</span> · {s.nomeBairro || '—'} · {formatDate(s.dataAbertura)}
                </div>
              </div>
              <StatusBadge status={s.status}/>
            </Link>
          ))}
        </Card>
      )}
    </div>
  )
}
