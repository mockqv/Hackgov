import { Link } from 'react-router-dom'
import { ShieldOff } from 'lucide-react'
import { useAuth } from '../../contexts/AuthContext'
import { homeForRole } from '../../router/roles'

export default function Forbidden() {
  const { user } = useAuth()
  const home = homeForRole(user?.perfil)
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-950 px-6 text-center">
      <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center mb-5">
        <ShieldOff size={28} className="text-amber-400" />
      </div>
      <h1 className="text-2xl font-bold text-white">403 · Acesso negado</h1>
      <p className="text-slate-400 text-sm mt-2 max-w-sm">
        Seu perfil ({user?.perfil || 'visitante'}) não tem permissão para acessar essa página.
      </p>
      <Link to={home}
        className="mt-6 px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold transition-colors">
        Voltar para o início
      </Link>
    </div>
  )
}
