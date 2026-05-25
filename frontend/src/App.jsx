import { Toaster } from 'sonner'
import { AuthProvider } from './contexts/AuthContext'
import AppRouter from './router/AppRouter'

export default function App() {
  return (
    <AuthProvider>
      <AppRouter />
      <Toaster
        position="top-right"
        theme="dark"
        toastOptions={{
          classNames: {
            toast: 'bg-slate-900 border border-slate-700 text-slate-100',
            success: 'border-green-700/40',
            error: 'border-red-700/40',
            description: 'text-slate-400',
          },
        }}
      />
    </AuthProvider>
  )
}
