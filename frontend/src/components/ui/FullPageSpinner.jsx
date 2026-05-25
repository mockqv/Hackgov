import { Loader2 } from 'lucide-react'

export default function FullPageSpinner({ label = 'Carregando...' }) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-950 text-slate-400">
      <Loader2 size={32} className="animate-spin text-blue-500 mb-3" />
      <p className="text-sm">{label}</p>
    </div>
  )
}
