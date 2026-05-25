import { Loader2 } from 'lucide-react'
import { forwardRef } from 'react'
import { cn } from '../../lib/cn'

const VARIANTS = {
  primary:
    'bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white border-transparent shadow-sm shadow-blue-900/40',
  secondary:
    'bg-slate-800 hover:bg-slate-700 active:bg-slate-700/80 text-slate-100 border-slate-700',
  ghost:
    'bg-transparent hover:bg-slate-800/60 text-slate-300 border-transparent',
  danger:
    'bg-red-600 hover:bg-red-500 active:bg-red-700 text-white border-transparent shadow-sm shadow-red-900/40',
  outline:
    'bg-transparent hover:bg-slate-800/60 text-slate-200 border-slate-700',
}

const SIZES = {
  sm: 'h-8  px-3   text-xs gap-1.5',
  md: 'h-10 px-4   text-sm gap-2',
  lg: 'h-12 px-5   text-base gap-2',
}

const Button = forwardRef(function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  className,
  children,
  type = 'button',
  ...props
}, ref) {
  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center rounded-xl border font-semibold',
        'transition-all duration-150 active:scale-[0.98]',
        'disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100',
        VARIANTS[variant],
        SIZES[size],
        className
      )}
      {...props}>
      {loading && <Loader2 size={14} className="animate-spin"/>}
      {children}
    </button>
  )
})

export default Button
