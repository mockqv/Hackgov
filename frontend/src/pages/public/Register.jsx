import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { User, Mail, Lock, CreditCard, Phone } from 'lucide-react'

import { useAuth } from '../../contexts/AuthContext'
import { RegisterSchema } from '../../lib/validation'
import { homeForRole } from '../../router/roles'
import { maskCpf, maskPhone } from '../../lib/format'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'

export default function Register() {
  const { register: doRegister } = useAuth()
  const navigate = useNavigate()

  const {
    register, handleSubmit, control,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(RegisterSchema),
    mode: 'onTouched',
    defaultValues: { nome:'', email:'', cpf:'', telefone:'', senha:'', confirmaSenha:'' },
  })

  // DEBUG: loga os erros de validação que estão impedindo o submit
  const onValidationError = (errs) => {
    // eslint-disable-next-line no-console
    console.warn('[Register] Erros de validação bloqueando submit:', errs)
  }

  async function onSubmit(values) {
    try {
      const user = await doRegister({
        nome:     values.nome.trim(),
        email:    values.email.trim().toLowerCase(),
        cpf:      values.cpf,            // já é normalizado pelo zod
        telefone: values.telefone || null,
        senha:    values.senha,
      })
      toast.success(`Conta criada! Olá, ${user.nome.split(' ')[0]}.`)
      navigate(homeForRole(user.perfil), { replace: true })
    } catch (err) {
      toast.error('Falha no cadastro', { description: err.message })
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="text-center mb-7">
          <h1 className="text-2xl font-bold text-white">Criar sua conta</h1>
          <p className="text-slate-400 text-sm mt-1.5">
            Abra solicitações de zeladoria e acompanhe seu protocolo.
          </p>
        </div>

        <Card className="p-6">
          <form onSubmit={handleSubmit(onSubmit, onValidationError)} className="space-y-4" noValidate>
            <Input
              label="Nome completo"
              placeholder="João da Silva"
              leftIcon={<User size={15}/>}
              autoComplete="name"
              required
              error={errors.nome?.message}
              {...register('nome')}
            />

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

            <div className="grid grid-cols-2 gap-3">
              <Controller
                control={control}
                name="cpf"
                render={({ field }) => (
                  <Input
                    label="CPF"
                    placeholder="000.000.000-00"
                    leftIcon={<CreditCard size={15}/>}
                    inputMode="numeric"
                    autoComplete="off"
                    required
                    value={maskCpf(field.value || '')}
                    onChange={(e) => field.onChange(e.target.value.replace(/\D/g,''))}
                    onBlur={field.onBlur}
                    error={errors.cpf?.message}
                  />
                )}
              />
              <Controller
                control={control}
                name="telefone"
                render={({ field }) => (
                  <Input
                    label="Telefone"
                    placeholder="(11) 99999-9999"
                    leftIcon={<Phone size={15}/>}
                    inputMode="tel"
                    autoComplete="tel"
                    value={maskPhone(field.value || '')}
                    onChange={(e) => field.onChange(e.target.value.replace(/\D/g,''))}
                    onBlur={field.onBlur}
                    error={errors.telefone?.message}
                  />
                )}
              />
            </div>

            <Input
              label="Senha"
              type="password"
              placeholder="Mínimo 8 caracteres com letras e números"
              leftIcon={<Lock size={15}/>}
              autoComplete="new-password"
              required
              error={errors.senha?.message}
              {...register('senha')}
            />

            <Input
              label="Confirmar senha"
              type="password"
              placeholder="Repita a senha"
              leftIcon={<Lock size={15}/>}
              autoComplete="new-password"
              required
              error={errors.confirmaSenha?.message}
              {...register('confirmaSenha')}
            />

            <Button type="submit" loading={isSubmitting} className="w-full mt-2">
              Criar conta
            </Button>
          </form>
        </Card>

        <p className="text-center text-sm text-slate-400 mt-6">
          Já tem conta?{' '}
          <Link to="/login" className="text-blue-400 hover:text-blue-300 font-semibold">
            Entrar
          </Link>
        </p>
      </div>
    </div>
  )
}
