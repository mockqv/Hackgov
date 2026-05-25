import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-950 px-6 text-center">
      <div className="w-16 h-16 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center mb-5">
        <Compass size={28} className="text-blue-400" />
      </div>
      <h1 className="text-2xl font-bold text-white">404 · Página não encontrada</h1>
      <p className="text-slate-400 text-sm mt-2">A rota que você tentou acessar não existe.</p>
      <Link to="/"
        className="mt-6 px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold transition-colors">
        Ir para o início
      </Link>
    </div>
  )
}
