import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import FullPageSpinner from '../components/ui/FullPageSpinner'
import { homeForRole } from './roles'

/** Define para onde mandar quem acessa "/" — depende de estar logado e do perfil. */
export default function RootRedirect() {
  const { isLoading, isAuthenticated, user } = useAuth()
  if (isLoading) return <FullPageSpinner />
  if (!isAuthenticated) return <Navigate to="/mapa-publico" replace />
  return <Navigate to={homeForRole(user?.perfil)} replace />
}
