import { Loader2 } from 'lucide-react'
import { forwardRef } from 'react'
import { cn } from '../../lib/cn'

const VARIANTS = {
  primary:
    'bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white border-transparent shadow-sm shadow-blue-600/20',
  secondary:
    'bg-slate-100 hover:bg-slate-200 active:bg-slate-200 text-slate-700 border-slate-200',
  ghost:
    'bg-transparent hover:bg-slate-100 text-slate-600 border-transparent',
  danger:
    'bg-red-600 hover:bg-red-700 active:bg-red-800 text-white border-transparent shadow-sm shadow-red-600/20',
  outline:
    'bg-white hover:bg-slate-50 text-slate-700 border-slate-300',
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
        'transition-all duration-150 active:scale-[0.98] cursor-pointer',
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
