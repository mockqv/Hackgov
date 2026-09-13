import { Toaster } from 'sonner'
import { AuthProvider } from './contexts/AuthContext'
import AppRouter from './router/AppRouter'

export default function App() {
  return (
    <AuthProvider>
      <AppRouter />
      <Toaster
        position="top-right"
        theme="light"
        toastOptions={{
          classNames: {
            toast: 'bg-white border border-slate-200 text-slate-900 shadow-lg',
            success: 'border-emerald-200',
            error: 'border-red-200',
            description: 'text-slate-500',
          },
        }}
      />
    </AuthProvider>
  )
}
