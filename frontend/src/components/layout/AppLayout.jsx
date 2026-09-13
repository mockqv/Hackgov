import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { PERFIL } from '../../router/roles'
import { getInitials } from '../../lib/format'
import {
  Home, Plus, FileText, ClipboardList, BarChart3, MapPin, LogOut, Settings2
} from 'lucide-react'
import { cn } from '../../lib/cn'

const NAV = {
  [PERFIL.CIDADAO]: [
    { to: '/inicio',              icon: Home,         label: 'Início' },
    { to: '/nova-solicitacao',    icon: Plus,         label: 'Nova solicitação' },
    { to: '/acompanhar',          icon: FileText,     label: 'Acompanhar' },
    { to: '/minhas-solicitacoes', icon: ClipboardList,label: 'Minhas solicitações' },
    { to: '/mapa-publico',        icon: MapPin,       label: 'Mapa público' },
  ],
  [PERFIL.SERVIDOR]: [
    { to: '/painel',    icon: ClipboardList, label: 'Painel de triagem' },
    { to: '/dashboard', icon: BarChart3,     label: 'Dashboard' },
  ],
  [PERFIL.GESTOR]: [
    { to: '/painel',         icon: ClipboardList, label: 'Painel de triagem' },
    { to: '/dashboard',      icon: BarChart3,     label: 'Dashboard' },
    { to: '/tipos-servico',  icon: Settings2,     label: 'Tipos de serviço' },
  ],
}

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const nav = NAV[user?.perfil] || []

  const handleLogout = () => { logout(); navigate('/login') }

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 text-slate-900">
      <aside className="w-60 flex-shrink-0 bg-white border-r border-slate-200 flex flex-col">
        {/* Brand */}
        <div className="h-14 flex items-center gap-2.5 px-4 border-b border-slate-200">
          <div className="w-7 h-7 rounded-lg bg-blue-600 flex items-center justify-center font-black text-sm text-white">H</div>
          <div>
            <div className="text-sm font-bold font-display leading-none text-slate-900">HackGov</div>
            <div className="text-[10px] text-slate-500 leading-none mt-0.5">Zeladoria Urbana</div>
          </div>
        </div>

        {/* User pill */}
        {user && (
          <div className="m-3 p-3 rounded-xl bg-slate-50 border border-slate-200 flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-700 text-xs font-bold">
              {getInitials(user.nome)}
            </div>
            <div className="min-w-0">
              <div className="text-xs font-semibold text-slate-900 truncate">{user.nome}</div>
              <div className="text-[10px] text-slate-500 mt-0.5 capitalize">{user.perfil?.toLowerCase()}</div>
            </div>
          </div>
        )}

        {/* Nav */}
        <nav className="flex-1 px-2 py-1 space-y-0.5 overflow-y-auto">
          {nav.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to}
              className={({ isActive }) => cn(
                'flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] font-medium transition-colors',
                isActive
                  ? 'bg-blue-50 text-blue-700 border border-blue-200'
                  : 'text-slate-500 hover:text-slate-900 hover:bg-slate-100 border border-transparent'
              )}>
              <Icon size={15}/>
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        {/* Logout */}
        <div className="p-3 border-t border-slate-200">
          <button onClick={handleLogout}
            className="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] font-medium cursor-pointer
                       text-slate-500 hover:text-red-600 hover:bg-red-50 transition-colors">
            <LogOut size={15}/>
            Sair
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto flex flex-col">
        <Outlet />
      </main>
    </div>
  )
}
