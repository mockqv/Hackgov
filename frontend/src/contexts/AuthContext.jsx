import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { authApi, TOKEN_KEY, onAuthLogout } from '../lib/api'

const AuthContext = createContext(null)

const Status = {
  LOADING:  'loading',  // boot: verificando token guardado
  AUTH:     'authenticated',
  GUEST:    'guest',
}

export function AuthProvider({ children }) {
  const [user,   setUser]   = useState(null)
  const [status, setStatus] = useState(Status.LOADING)

  // ── bootstrap: tenta validar token guardado ───────────────
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) { setStatus(Status.GUEST); return }

    authApi.me()
      .then(res => {
        setUser(res.data)
        setStatus(Status.AUTH)
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY)
        setUser(null)
        setStatus(Status.GUEST)
      })
  }, [])

  // ── escuta 401 global do interceptor ──────────────────────
  useEffect(() => {
    return onAuthLogout(() => {
      setUser(null)
      setStatus(Status.GUEST)
    })
  }, [])

  const login = useCallback(async (credentials) => {
    const res = await authApi.login(credentials)
    const { token, user } = res.data
    localStorage.setItem(TOKEN_KEY, token)
    setUser(user)
    setStatus(Status.AUTH)
    return user
  }, [])

  const register = useCallback(async (data) => {
    const res = await authApi.register(data)
    const { token, user } = res.data
    localStorage.setItem(TOKEN_KEY, token)
    setUser(user)
    setStatus(Status.AUTH)
    return user
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
    setStatus(Status.GUEST)
  }, [])

  const value = {
    user,
    isLoading:       status === Status.LOADING,
    isAuthenticated: status === Status.AUTH,
    isGuest:         status === Status.GUEST,
    hasRole:         (perfil) => user?.perfil === perfil,
    hasAnyRole:      (...perfis) => perfis.includes(user?.perfil),
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve estar dentro de <AuthProvider>')
  return ctx
}
