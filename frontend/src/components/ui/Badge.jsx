import { cn } from '../../lib/cn'

const VARIANTS = {
  neutral: 'bg-slate-800/80 text-slate-300 border-slate-700',
  red:     'bg-red-500/10    text-red-300    border-red-500/30',
  amber:   'bg-amber-500/10  text-amber-300  border-amber-500/30',
  blue:    'bg-blue-500/10   text-blue-300   border-blue-500/30',
  green:   'bg-green-500/10  text-green-300  border-green-500/30',
  slate:   'bg-slate-500/10  text-slate-300  border-slate-500/30',
}

export default function Badge({ variant = 'neutral', className, children }) {
  return (
    <span className={cn(
      'inline-flex items-center gap-1 px-2 py-0.5 rounded-md',
      'text-[10.5px] font-bold uppercase tracking-wide border',
      VARIANTS[variant], className,
    )}>
      {children}
    </span>
  )
}
