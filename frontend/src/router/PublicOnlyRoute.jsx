import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import FullPageSpinner from '../components/ui/FullPageSpinner'
import { homeForRole } from './roles'

/** Rotas só para visitantes (login, cadastro). Logados são jogados pro home do perfil. */
export default function PublicOnlyRoute({ children }) {
  const { isLoading, isAuthenticated, user } = useAuth()

  if (isLoading) return <FullPageSpinner label="Carregando..." />

  if (isAuthenticated) {
    return <Navigate to={homeForRole(user?.perfil)} replace />
  }

  return children
}
