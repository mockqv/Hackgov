import { useAuth } from '../../contexts/AuthContext'
import AppLayout from './AppLayout'
import PublicLayout from './PublicLayout'
import FullPageSpinner from '../ui/FullPageSpinner'

/**
 * Layout adaptativo para rotas acessíveis por todos (ex: /mapa-publico).
 *
 * - Carregando         → spinner (aguarda resolução do token)
 * - Autenticado        → AppLayout (sidebar, sessão visível)
 * - Visitante          → PublicLayout (cabeçalho público)
 *
 * Ambos os layouts já possuem <Outlet />, então o React Router
 * injeta o componente filho normalmente em qualquer caso.
 */
export default function SmartLayout() {
  const { isLoading, isAuthenticated } = useAuth()

  if (isLoading) return <FullPageSpinner label="Carregando..." />
  if (isAuthenticated) return <AppLayout />
  return <PublicLayout />
}
