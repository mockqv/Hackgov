import { useEffect } from 'react'
import { X } from 'lucide-react'
import { cn } from '../../lib/cn'

export default function Modal({ open, onClose, title, children, footer, size = 'md' }) {
  // ESC fecha
  useEffect(() => {
    if (!open) return
    const onKey = (e) => e.key === 'Escape' && onClose?.()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  // trava scroll do body
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [open])

  if (!open) return null

  const widths = { sm: 'max-w-sm', md: 'max-w-md', lg: 'max-w-xl', xl: 'max-w-3xl' }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in"
      onClick={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className={cn(
        'w-full bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl',
        'flex flex-col max-h-[90vh]',
        widths[size],
      )}>
        <div className="flex items-start justify-between gap-3 px-5 py-4 border-b border-slate-800">
          <h3 className="text-sm font-bold text-slate-100">{title}</h3>
          <button onClick={onClose}
            aria-label="Fechar"
            className="text-slate-500 hover:text-slate-200 transition-colors -mr-1 -mt-1 p-1 rounded-md hover:bg-slate-800">
            <X size={16}/>
          </button>
        </div>
        <div className="p-5 overflow-y-auto">{children}</div>
        {footer && (
          <div className="px-5 py-3.5 border-t border-slate-800 flex justify-end gap-2.5">
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}
