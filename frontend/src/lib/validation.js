import { z } from 'zod'
import { isValidCpf, unmaskCpf, unmaskPhone } from './format'

// ── AUTH ────────────────────────────────────────────────────
export const LoginSchema = z.object({
  email: z.string().min(1, 'E-mail obrigatório').email('E-mail inválido'),
  senha: z.string().min(1, 'Senha obrigatória'),
})

export const RegisterSchema = z.object({
  nome:     z.string().min(3, 'Mínimo 3 caracteres').max(150, 'Máximo 150 caracteres'),
  email:    z.string().min(1, 'E-mail obrigatório').email('E-mail inválido'),
  cpf:      z.string().transform(unmaskCpf).refine(isValidCpf, 'CPF inválido'),
  telefone: z.string().transform(unmaskPhone)
              .refine(v => v === '' || /^\d{10,11}$/.test(v), 'Telefone inválido'),
  senha:    z.string()
              .min(8, 'Mínimo 8 caracteres')
              .max(72, 'Máximo 72 caracteres')
              .regex(/[A-Za-z]/, 'Precisa de letra')
              .regex(/\d/,      'Precisa de número'),
  confirmaSenha: z.string(),
}).refine(d => d.senha === d.confirmaSenha, {
  path: ['confirmaSenha'],
  message: 'Senhas não conferem',
})

// ── SOLICITAÇÃO ─────────────────────────────────────────────
export const SolicitacaoSchema = z.object({
  tipoServicoId:    z.coerce.number({ invalid_type_error: 'Selecione um tipo' }).int().positive('Selecione um tipo'),
  descricao:        z.string().min(10, 'Mínimo 10 caracteres').max(500, 'Máximo 500 caracteres'),
  latitude:         z.coerce.number().min(-90).max(90),
  longitude:        z.coerce.number().min(-180).max(180),
  logradouro:       z.string().max(200).optional().or(z.literal('')),
  numero:           z.string().max(20).optional().or(z.literal('')),
  complemento:      z.string().max(100).optional().or(z.literal('')),
  cep:              z.string().transform(v => v.replace(/\D/g, ''))
                      .refine(v => v === '' || /^\d{8}$/.test(v), 'CEP inválido'),
  bairroId:         z.coerce.number({ invalid_type_error: 'Selecione um bairro' }).int().positive('Selecione um bairro'),
  caminhoFotoAntes: z.string().max(300).optional().or(z.literal('')),
})

// ── ATUALIZAR STATUS ────────────────────────────────────────
export const AtualizarStatusSchema = z.object({
  novoStatus:        z.enum(['EM_ANALISE','AGENDADO','EM_EXECUCAO','CONCLUIDO','CANCELADO'], {
                       errorMap: () => ({ message: 'Selecione um status' })
                     }),
  justificativa:     z.string().max(500).optional().or(z.literal('')),
  caminhoFotoDepois: z.string().max(300).optional().or(z.literal('')),
}).superRefine((d, ctx) => {
  if (d.novoStatus === 'CONCLUIDO' && !d.caminhoFotoDepois) {
    ctx.addIssue({ code: 'custom', path: ['caminhoFotoDepois'],
                   message: 'Foto do depois é obrigatória para concluir' })
  }
  if (d.novoStatus === 'CANCELADO' && !d.justificativa) {
    ctx.addIssue({ code: 'custom', path: ['justificativa'],
                   message: 'Justificativa é obrigatória para cancelar' })
  }
})

// ── AVALIAÇÃO ───────────────────────────────────────────────
export const AvaliacaoSchema = z.object({
  nota:       z.number().int().min(1, 'Selecione uma nota').max(5),
  comentario: z.string().max(500).optional().or(z.literal('')),
})
