import { cn } from '../../lib/cn'

const VARIANTS = {
  neutral: 'bg-slate-100  text-slate-600   border-slate-200',
  red:     'bg-red-50     text-red-700     border-red-200',
  amber:   'bg-amber-50   text-amber-700   border-amber-200',
  blue:    'bg-blue-50    text-blue-700    border-blue-200',
  green:   'bg-emerald-50 text-emerald-700 border-emerald-200',
  slate:   'bg-slate-100  text-slate-500   border-slate-200',
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
