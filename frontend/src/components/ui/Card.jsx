import { cn } from '../../lib/cn'

export default function Card({ className, children, ...props }) {
  return (
    <div
      className={cn(
        'bg-white border border-slate-200 rounded-2xl shadow-card',
        className,
      )}
      {...props}>
      {children}
    </div>
  )
}
