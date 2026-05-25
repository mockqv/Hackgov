import { forwardRef } from 'react'
import { cn } from '../../lib/cn'

const Field = forwardRef(function Field({
  as = 'input',
  label,
  hint,
  error,
  required = false,
  className,
  containerClassName,
  leftIcon,
  rightSlot,
  id,
  ...props
}, ref) {
  const Tag = as
  const fieldId = id || (label ? `f-${label.replace(/\s+/g, '-')}` : undefined)

  const base = cn(
    'w-full bg-slate-900/70 border rounded-xl text-sm text-slate-100',
    'placeholder-slate-500 transition-colors',
    'focus:outline-none focus:ring-2 focus:ring-blue-500/40',
    error
      ? 'border-red-500/60 focus:border-red-500'
      : 'border-slate-700 focus:border-blue-500',
    leftIcon ? 'pl-10 pr-3.5' : 'px-3.5',
    as === 'textarea' ? 'py-2.5 resize-y min-h-[88px]' : 'h-10 py-2',
    'disabled:opacity-60 disabled:cursor-not-allowed',
    className,
  )

  return (
    <div className={cn('flex flex-col gap-1.5', containerClassName)}>
      {label && (
        <label htmlFor={fieldId} className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
          {label}
          {required && <span className="text-red-400 ml-0.5">*</span>}
        </label>
      )}
      <div className="relative">
        {leftIcon && (
          <span className="absolute inset-y-0 left-3 flex items-center text-slate-500 pointer-events-none">
            {leftIcon}
          </span>
        )}
        <Tag id={fieldId} ref={ref} className={base} {...props} />
        {rightSlot && (
          <span className="absolute inset-y-0 right-2.5 flex items-center">
            {rightSlot}
          </span>
        )}
      </div>
      {error && <p className="text-[11px] text-red-400 leading-tight">{error}</p>}
      {!error && hint && <p className="text-[11px] text-slate-500 leading-tight">{hint}</p>}
    </div>
  )
})

// IMPORTANTE: forwardRef em todos os wrappers, senão o `ref` do react-hook-form
// (vindo de `register()`) é descartado pelo React e o form fica "vazio".
export const Input = forwardRef(function Input(props, ref) {
  return <Field as="input" ref={ref} {...props} />
})

export const Textarea = forwardRef(function Textarea(props, ref) {
  return <Field as="textarea" ref={ref} {...props} />
})

export const Select = forwardRef(function Select({ children, ...props }, ref) {
  return <Field as="select" ref={ref} {...props}>{children}</Field>
})

export default Input
