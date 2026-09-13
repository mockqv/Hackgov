import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 px-6 text-center">
      <div className="w-16 h-16 rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-center mb-5">
        <Compass size={28} className="text-blue-600" />
      </div>
      <h1 className="text-2xl font-bold font-display text-slate-900">404 · Página não encontrada</h1>
      <p className="text-slate-500 text-sm mt-2">A rota que você tentou acessar não existe.</p>
      <Link to="/"
        className="mt-6 px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold transition-colors">
        Ir para o início
      </Link>
    </div>
  )
}
