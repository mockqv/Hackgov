import { useRef, useState } from 'react'
import { Upload, Image as ImgIcon, X, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '../../lib/cn'
import { arquivoApi } from '../../lib/api'

const MAX_BYTES = 5 * 1024 * 1024 // 5 MB
const TIPOS_ACEITOS = ['image/jpeg', 'image/png', 'image/webp']

/**
 * Upload de foto (Fase 6): envia o arquivo ao backend (POST /api/arquivos, multipart)
 * e devolve via onChange(string) o caminho curto gravado na solicitação
 * ("/api/arquivos/<uuid>.jpg").
 *
 * Antes a imagem era convertida em data-URL base64 e enviada como texto num campo
 * de 300 caracteres — qualquer foto real estourava o limite e o envio era recusado.
 * Validações no cliente (tipo e tamanho) só melhoram a experiência; o backend
 * repete as checagens e confere a assinatura binária do arquivo.
 */
export default function PhotoUpload({ label = 'Foto', value, onChange, error, required = false }) {
  const inputRef = useRef(null)
  const [dragOver, setDragOver] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const handleFiles = async (files) => {
    const file = files?.[0]
    if (!file) return
    if (!TIPOS_ACEITOS.includes(file.type)) {
      toast.error('Envie uma imagem JPG, PNG ou WEBP')
      return
    }
    if (file.size > MAX_BYTES) {
      toast.error('Imagem muito grande (máx 5 MB)')
      return
    }
    setEnviando(true)
    try {
      const res = await arquivoApi.enviarImagem(file)
      onChange?.(res.data.caminho)
      toast.success('Foto enviada')
    } catch (err) {
      toast.error('Falha ao enviar a foto', { description: err.message })
    } finally {
      setEnviando(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  const isImagem = typeof value === 'string' && (value.startsWith('/api/arquivos/') || value.startsWith('data:image'))

  return (
    <div className="space-y-1.5">
      <label className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
        {label}{required && <span className="text-red-500 ml-0.5">*</span>}
      </label>

      {value ? (
        <div className="relative rounded-xl overflow-hidden border border-slate-200 bg-slate-50">
          {isImagem ? (
            <img src={value} alt="Pré-visualização" className="w-full h-48 object-cover"/>
          ) : (
            <div className="flex items-center gap-2 p-3 text-xs text-slate-500 font-mono truncate">
              <ImgIcon size={14}/> {value}
            </div>
          )}
          <button type="button" onClick={() => onChange?.('')}
            aria-label="Remover foto"
            className="absolute top-2 right-2 p-1.5 rounded-lg bg-slate-900/70 hover:bg-slate-900/90 text-white">
            <X size={13}/>
          </button>
        </div>
      ) : (
        <div
          role="button"
          tabIndex={0}
          aria-busy={enviando}
          onClick={() => !enviando && inputRef.current?.click()}
          onKeyDown={(e) => !enviando && (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
          onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragOver(false)
            if (!enviando) handleFiles(e.dataTransfer?.files)
          }}
          className={cn(
            'flex flex-col items-center justify-center gap-2 px-4 py-6 rounded-xl',
            'border border-dashed transition-colors',
            enviando ? 'cursor-wait opacity-70' : 'cursor-pointer',
            dragOver
              ? 'border-blue-500 bg-blue-50'
              : error
                ? 'border-red-400 hover:border-red-500'
                : 'border-slate-300 hover:border-slate-400 bg-slate-50',
          )}>
          {enviando
            ? <Loader2 size={20} className="text-blue-600 animate-spin"/>
            : <Upload size={20} className="text-slate-500"/>}
          <div className="text-center">
            <div className="text-xs font-semibold text-slate-600">
              {enviando ? 'Enviando foto...' : 'Clique ou arraste uma imagem'}
            </div>
            <div className="text-[10px] text-slate-500 mt-0.5">PNG, JPG ou WEBP — até 5 MB</div>
          </div>
        </div>
      )}
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={(e) => handleFiles(e.target.files)}
      />
      {error && <p className="text-[11px] text-red-600">{error}</p>}
    </div>
  )
}
