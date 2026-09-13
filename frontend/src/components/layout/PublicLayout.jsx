import { Outlet, Link } from 'react-router-dom'
import { MapPin } from 'lucide-react'

export default function PublicLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      <header className="h-14 border-b border-slate-200 bg-white px-4 sm:px-5 flex items-center justify-between gap-2">
        <Link to="/" className="flex items-center gap-2.5 flex-shrink-0">
          <div className="w-7 h-7 rounded-lg bg-blue-600 flex items-center justify-center font-black text-sm text-white">H</div>
          <div className="text-sm font-bold font-display tracking-tight text-slate-900">HackGov</div>
        </Link>
        <nav className="flex items-center gap-1.5 sm:gap-3 text-xs font-medium flex-shrink-0">
          <Link to="/mapa-publico" aria-label="Mapa público"
            className="text-slate-500 hover:text-slate-900 hover:bg-slate-100 flex items-center gap-1.5 p-2 sm:px-2 sm:py-1.5 rounded-lg transition-colors">
            <MapPin size={14}/> <span className="hidden sm:inline">Mapa público</span>
          </Link>
          <Link to="/login" className="text-slate-500 hover:text-slate-900 px-2 py-1.5 rounded-lg hover:bg-slate-100 transition-colors">Entrar</Link>
          <Link to="/cadastro" className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white transition-colors whitespace-nowrap">
            Criar conta
          </Link>
        </nav>
      </header>
      <main className="flex-1 flex flex-col">
        <Outlet />
      </main>
    </div>
  )
}
