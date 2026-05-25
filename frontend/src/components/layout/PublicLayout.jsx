import { Outlet, Link } from 'react-router-dom'
import { MapPin } from 'lucide-react'

export default function PublicLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      <header className="h-14 border-b border-slate-800 px-5 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-lg bg-blue-600 flex items-center justify-center font-black text-sm">H</div>
          <div className="text-sm font-bold tracking-tight">HackGov</div>
        </Link>
        <nav className="flex items-center gap-3 text-xs font-medium">
          <Link to="/mapa-publico" className="text-slate-400 hover:text-white flex items-center gap-1.5">
            <MapPin size={13}/> Mapa público
          </Link>
          <Link to="/login" className="text-slate-400 hover:text-white">Entrar</Link>
          <Link to="/cadastro" className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white">
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
