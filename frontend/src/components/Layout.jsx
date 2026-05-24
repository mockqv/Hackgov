import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext'
import {
  MapPin, Plus, FileText, BarChart3,
  ClipboardList, LogOut, ChevronRight
} from 'lucide-react'

const cidadaoNav = [
  { to: '/mapa',         icon: MapPin,        label: 'Mapa público'     },
  { to: '/nova',         icon: Plus,          label: 'Nova solicitação' },
  { to: '/acompanhar',   icon: FileText,      label: 'Acompanhar'       },
]

const servidorNav = [
  { to: '/painel',       icon: ClipboardList, label: 'Painel de triagem' },
  { to: '/dashboard',    icon: BarChart3,     label: 'Dashboard'          },
]

export default function Layout() {
  const { user, logout } = useApp()
  const navigate = useNavigate()

  const nav = user?.role === 'servidor'
    ? servidorNav
    : user?.role === 'cidadao'
      ? cidadaoNav
      : [...cidadaoNav, ...servidorNav]

  const handleLogout = () => { logout(); navigate('/login') }

  return (
    <div className="flex h-screen overflow-hidden">

      {/* ── SIDEBAR ── */}
      <aside className="w-60 flex-shrink-0 bg-gray-950 border-r border-gray-800
                        flex flex-col">
        {/* Logo */}
        <div className="h-16 flex items-center gap-3 px-5 border-b border-gray-800">
          <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center
                          text-white font-black text-sm">H</div>
          <div>
            <div className="text-sm font-bold text-white leading-none">HackGov</div>
            <div className="text-[10px] text-gray-500 leading-none mt-0.5">Zeladoria Urbana</div>
          </div>
        </div>

        {/* User pill */}
        {user && (
          <div className="mx-3 mt-4 p-3 rounded-xl bg-gray-900 border border-gray-800">
            <div className="text-xs font-semibold text-white truncate">{user.nome}</div>
            <div className="text-[10px] text-gray-500 mt-0.5 capitalize">{user.role}</div>
          </div>
        )}

        {/* Nav */}
        <nav className="flex-1 p-3 space-y-1 overflow-y-auto mt-2">
          {nav.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium
                 transition-all duration-150
                 ${isActive
                   ? 'bg-blue-600/20 text-blue-400 border border-blue-500/20'
                   : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'}`
              }>
              <Icon size={16} />
              <span className="flex-1">{label}</span>
              <ChevronRight size={12} className="opacity-40" />
            </NavLink>
          ))}
        </nav>

        {/* Logout */}
        <div className="p-3 border-t border-gray-800">
          <button onClick={handleLogout}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl
                       text-sm font-medium text-gray-500 hover:text-red-400
                       hover:bg-red-950/30 transition-all duration-150">
            <LogOut size={16} />
            Sair
          </button>
        </div>
      </aside>

      {/* ── MAIN CONTENT ── */}
      <main className="flex-1 overflow-y-auto bg-gray-950">
        <Outlet />
      </main>
    </div>
  )
}
