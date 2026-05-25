// ── Máscaras ────────────────────────────────────────────────
export const maskCpf = (v = '') =>
  v.replace(/\D/g, '')
   .slice(0, 11)
   .replace(/(\d{3})(\d)/, '$1.$2')
   .replace(/(\d{3})(\d)/, '$1.$2')
   .replace(/(\d{3})(\d{1,2})$/, '$1-$2')

export const unmaskCpf = (v = '') => v.replace(/\D/g, '')

export const maskCep = (v = '') =>
  v.replace(/\D/g, '')
   .slice(0, 8)
   .replace(/(\d{5})(\d)/, '$1-$2')

export const unmaskCep = (v = '') => v.replace(/\D/g, '')

export const maskPhone = (v = '') => {
  const d = v.replace(/\D/g, '').slice(0, 11)
  if (d.length <= 10) {
    return d.replace(/(\d{2})(\d)/, '($1) $2').replace(/(\d{4})(\d)/, '$1-$2')
  }
  return d.replace(/(\d{2})(\d)/, '($1) $2').replace(/(\d{5})(\d)/, '$1-$2')
}

export const unmaskPhone = (v = '') => v.replace(/\D/g, '')

// ── Datas ───────────────────────────────────────────────────
export const formatDate = (iso) => {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleDateString('pt-BR')
  } catch { return '—' }
}

export const formatDateTime = (iso) => {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
  } catch { return '—' }
}

export const daysBetween = (a, b = new Date()) => {
  if (!a) return null
  const diff = (new Date(b) - new Date(a)) / 86400000
  return Math.floor(diff)
}

// ── Validador de CPF (algoritmo oficial) ────────────────────
export const isValidCpf = (cpf) => {
  const d = unmaskCpf(cpf)
  if (d.length !== 11 || /^(\d)\1+$/.test(d)) return false
  let sum = 0
  for (let i = 0; i < 9; i++) sum += parseInt(d[i]) * (10 - i)
  let rev = 11 - (sum % 11)
  if (rev >= 10) rev = 0
  if (rev !== parseInt(d[9])) return false
  sum = 0
  for (let i = 0; i < 10; i++) sum += parseInt(d[i]) * (11 - i)
  rev = 11 - (sum % 11)
  if (rev >= 10) rev = 0
  return rev === parseInt(d[10])
}

// ── Iniciais para Avatar ────────────────────────────────────
export const getInitials = (nome = '') =>
  nome.trim().split(/\s+/).slice(0, 2).map(p => p[0]?.toUpperCase() || '').join('') || '?'
