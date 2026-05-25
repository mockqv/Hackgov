import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { Mail, Lock } from 'lucide-react'

import { useAuth } from '../../contexts/AuthContext'
import { LoginSchema } from '../../lib/validation'
import { homeForRole } from '../../router/roles'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from

  const {
    register, handleSubmit, formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(LoginSchema) })

  async function onSubmit(values) {
    try {
      const user = await login({ email: values.email.trim().toLowerCase(), senha: values.senha })
      toast.success(`Bem-vindo(a), ${user.nome.split(' ')[0]}!`)
      navigate(from || homeForRole(user.perfil), { replace: true })
    } catch (err) {
      toast.error('Falha no login', { description: err.message })
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="text-center mb-7">
          <h1 className="text-2xl font-bold text-white">Entrar na sua conta</h1>
          <p className="text-slate-400 text-sm mt-1.5">
            Acompanhe e gerencie solicitações de zeladoria urbana.
          </p>
        </div>

        <Card className="p-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <Input
              label="E-mail"
              type="email"
              placeholder="seu@email.com"
              leftIcon={<Mail size={15}/>}
              autoComplete="email"
              required
              error={errors.email?.message}
              {...register('email')}
            />

            <Input
              label="Senha"
              type="password"
              placeholder="••••••••"
              leftIcon={<Lock size={15}/>}
              autoComplete="current-password"
              required
              error={errors.senha?.message}
              {...register('senha')}
            />

            <Button type="submit" loading={isSubmitting} className="w-full">
              Entrar
            </Button>
          </form>
        </Card>

        <p className="text-center text-sm text-slate-400 mt-6">
          Ainda não tem conta?{' '}
          <Link to="/cadastro" className="text-blue-400 hover:text-blue-300 font-semibold">
            Criar conta
          </Link>
        </p>
      </div>
    </div>
  )
}
