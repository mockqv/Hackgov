import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import FullPageSpinner from '../components/ui/FullPageSpinner'

/** Exige usuário autenticado. Se for visitante, redireciona para /login com `from`. */
export default function PrivateRoute({ children }) {
  const { isLoading, isAuthenticated } = useAuth()
  const location = useLocation()

  if (isLoading) return <FullPageSpinner label="Verificando sessão..." />

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return children
}
