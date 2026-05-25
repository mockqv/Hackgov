import Badge from './Badge'

const MAP = {
  RECEBIDO:    { variant: 'red',    label: 'Recebido'    },
  EM_ANALISE:  { variant: 'amber',  label: 'Em análise'  },
  AGENDADO:    { variant: 'amber',  label: 'Agendado'    },
  EM_EXECUCAO: { variant: 'blue',   label: 'Em execução' },
  CONCLUIDO:   { variant: 'green',  label: 'Concluído'   },
  CANCELADO:   { variant: 'slate',  label: 'Cancelado'   },
}

export default function StatusBadge({ status, className }) {
  const cfg = MAP[status] || { variant: 'neutral', label: status }
  return <Badge variant={cfg.variant} className={className}>{cfg.label}</Badge>
}

export const STATUS_DOT = {
  RECEBIDO:    '#ef4444',
  EM_ANALISE:  '#f59e0b',
  AGENDADO:    '#f59e0b',
  EM_EXECUCAO: '#3b82f6',
  CONCLUIDO:   '#10b981',
  CANCELADO:   '#64748b',
}
