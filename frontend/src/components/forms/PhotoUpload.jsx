import { useRef, useState } from 'react'
import { Upload, Image as ImgIcon, X } from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '../../lib/cn'

const MAX_BYTES = 5 * 1024 * 1024 // 5 MB

/**
 * Upload de foto que converte para data-URL base64 e devolve via onChange(string).
 * Validações: tipo image/* e tamanho <= 5 MB.
 */
export default function PhotoUpload({ label = 'Foto', value, onChange, error, required = false }) {
  const inputRef = useRef(null)
  const [dragOver, setDragOver] = useState(false)

  const handleFiles = (files) => {
    const file = files?.[0]
    if (!file) return
    if (!file.type.startsWith('image/')) {
      toast.error('O arquivo precisa ser uma imagem')
      return
    }
    if (file.size > MAX_BYTES) {
      toast.error('Imagem muito grande (máx 5 MB)')
      return
    }
    const reader = new FileReader()
    reader.onload = () => onChange?.(reader.result)
    reader.readAsDataURL(file)
  }

  const isData = typeof value === 'string' && value.startsWith('data:image')

  return (
    <div className="space-y-1.5">
      <label className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
        {label}{required && <span className="text-red-400 ml-0.5">*</span>}
      </label>

      {value ? (
        <div className="relative rounded-xl overflow-hidden border border-slate-700 bg-slate-900/60">
          {isData ? (
            <img src={value} alt="Pré-visualização" className="w-full h-48 object-cover"/>
          ) : (
            <div className="flex items-center gap-2 p-3 text-xs text-slate-400 font-mono truncate">
              <ImgIcon size={14}/> {value}
            </div>
          )}
          <button type="button" onClick={() => onChange?.('')}
            className="absolute top-2 right-2 p-1.5 rounded-lg bg-black/60 hover:bg-black/80 text-white">
            <X size={13}/>
          </button>
        </div>
      ) : (
        <div
          role="button"
          tabIndex={0}
          onClick={() => inputRef.current?.click()}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
          onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragOver(false)
            handleFiles(e.dataTransfer?.files)
          }}
          className={cn(
            'flex flex-col items-center justify-center gap-2 px-4 py-6 rounded-xl',
            'border border-dashed cursor-pointer transition-colors',
            dragOver
              ? 'border-blue-500 bg-blue-500/10'
              : error
                ? 'border-red-500/50 hover:border-red-500'
                : 'border-slate-700 hover:border-slate-600 bg-slate-900/40',
          )}>
          <Upload size={20} className="text-slate-500"/>
          <div className="text-center">
            <div className="text-xs font-semibold text-slate-300">Clique ou arraste uma imagem</div>
            <div className="text-[10px] text-slate-500 mt-0.5">PNG, JPG ou WEBP — até 5 MB</div>
          </div>
        </div>
      )}
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => handleFiles(e.target.files)}
      />
      {error && <p className="text-[11px] text-red-400">{error}</p>}
    </div>
  )
}
