import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import FullPageSpinner from '../components/ui/FullPageSpinner'

/** Restringe rota por perfil. `roles` é um array de perfis aceitos. */
export default function RoleRoute({ roles = [], children }) {
  const { isLoading, isAuthenticated, user } = useAuth()

  if (isLoading) return <FullPageSpinner label="Carregando..." />
  if (!isAuthenticated) return <Navigate to="/login" replace />

  if (!roles.includes(user?.perfil)) {
    return <Navigate to="/403" replace />
  }
  return children
}
