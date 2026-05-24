import { createContext, useContext, useState } from 'react'

const AppContext = createContext(null)

export function AppProvider({ children }) {
  const [user, setUser] = useState(null)        // { id, nome, email, role: 'cidadao'|'servidor' }
  const [toast, setToast] = useState(null)      // { type: 'success'|'error', msg }

  const showToast = (type, msg) => {
    setToast({ type, msg })
    setTimeout(() => setToast(null), 3500)
  }

  const login = (userData) => setUser(userData)
  const logout = () => setUser(null)

  return (
    <AppContext.Provider value={{ user, login, logout, toast, showToast }}>
      {children}
      {toast && <Toast {...toast} />}
    </AppContext.Provider>
  )
}

function Toast({ type, msg }) {
  return (
    <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5
      rounded-2xl shadow-2xl text-sm font-semibold border animate-slide-up
      ${type === 'success'
        ? 'bg-green-950 border-green-700 text-green-300'
        : 'bg-red-950 border-red-700 text-red-300'}`}>
      <span>{type === 'success' ? '✓' : '✕'}</span>
      {msg}
    </div>
  )
}

export const useApp = () => useContext(AppContext)
