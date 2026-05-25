import { useState } from 'react'
import { Search, Loader2 } from 'lucide-react'
import { Input } from '../ui/Input'
import { maskCep } from '../../lib/format'
import { cepLookup } from '../../lib/api'

/**
 * Input de CEP com busca no ViaCEP.
 * onResolve recebe { logradouro, bairro, cidade, uf } quando o CEP é válido.
 */
export default function CepInput({ value, onChange, onResolve, error, ...rest }) {
  const [loading, setLoading] = useState(false)

  const tryLookup = async (raw) => {
    const digits = (raw || '').replace(/\D/g, '')
    if (digits.length !== 8) return
    setLoading(true)
    try {
      const result = await cepLookup(digits)
      if (result) onResolve?.(result)
    } finally { setLoading(false) }
  }

  return (
    <Input
      label="CEP"
      placeholder="00000-000"
      inputMode="numeric"
      autoComplete="postal-code"
      leftIcon={<Search size={14}/>}
      rightSlot={loading ? <Loader2 size={14} className="animate-spin text-blue-400"/> : null}
      value={maskCep(value || '')}
      onChange={(e) => {
        const v = e.target.value.replace(/\D/g, '')
        onChange?.(v)
        if (v.length === 8) tryLookup(v)
      }}
      error={error}
      {...rest}
    />
  )
}
