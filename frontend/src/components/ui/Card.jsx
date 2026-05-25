import { cn } from '../../lib/cn'

export default function Card({ className, children, ...props }) {
  return (
    <div
      className={cn(
        'bg-slate-900/70 border border-slate-800 rounded-2xl shadow-sm',
        className,
      )}
      {...props}>
      {children}
    </div>
  )
}
