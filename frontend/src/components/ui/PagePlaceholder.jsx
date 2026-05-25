import { Construction } from 'lucide-react'

/** Stub temporário usado pelas páginas ainda não migradas. */
export default function PagePlaceholder({ title, description }) {
  return (
    <div className="min-h-full flex flex-col items-center justify-center p-10 text-center">
      <div className="w-14 h-14 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center mb-4">
        <Construction size={24} className="text-amber-400"/>
      </div>
      <h1 className="text-lg font-bold text-white">{title}</h1>
      {description && <p className="text-slate-400 text-sm mt-1.5 max-w-md">{description}</p>}
    </div>
  )
}
