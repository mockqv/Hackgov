import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet'
import { statusIcon } from './leafletSetup'
import { Crosshair } from 'lucide-react'
import Button from '../ui/Button'

const DEFAULT = [-23.55052, -46.633308]

function RecenterControl({ to }) {
  const map = useMap()
  useEffect(() => {
    if (to) map.flyTo(to, 16)
  }, [to, map])
  return null
}

function ClickPin({ value, onChange, onUserMove }) {
  useMapEvents({
    click: (e) => {
      const next = { lat: e.latlng.lat, lng: e.latlng.lng }
      onChange(next)
      onUserMove?.(next)
    },
  })
  return value ? (
    <Marker
      position={[value.lat, value.lng]}
      icon={statusIcon('#3b82f6', true)}
      draggable
      eventHandlers={{
        dragend: (e) => {
          const { lat, lng } = e.target.getLatLng()
          const next = { lat, lng }
          onChange(next)
          onUserMove?.(next)
        },
      }}
    />
  ) : null
}

/**
 * Seletor de coordenadas. Permite:
 *  - clicar no mapa para definir um pin
 *  - arrastar o pin para refinar
 *  - usar localização do navegador
 *
 * Props:
 *  - value:        { lat, lng } | null   — posição atual do pin (controlled)
 *  - onChange:     ({lat,lng}) => void   — disparado a CADA mudança (user OU programática)
 *  - onUserMove:   ({lat,lng}) => void   — disparado SÓ quando user clica/arrasta/usa GPS
 *                                          Use para fazer reverse geocoding sem causar loop.
 */
export default function LocationPicker({ value, onChange, onUserMove, error, height = 280 }) {
  const [recenter, setRecenter] = useState(null)

  const handleGeolocate = () => {
    if (!('geolocation' in navigator)) return
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const next = { lat: pos.coords.latitude, lng: pos.coords.longitude }
        onChange(next)
        onUserMove?.(next)
        setRecenter([next.lat, next.lng])
      },
      () => {}, // ignora erros silenciosamente
      { enableHighAccuracy: true, timeout: 6000 },
    )
  }

  // Recentraliza quando `value` muda externamente (ex: CEP foi resolvido)
  useEffect(() => {
    if (value) setRecenter([value.lat, value.lng])
  }, [value?.lat, value?.lng])

  const center = value ? [value.lat, value.lng] : DEFAULT

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
          Local no mapa <span className="text-red-400 ml-0.5">*</span>
        </span>
        <Button type="button" variant="ghost" size="sm" onClick={handleGeolocate}>
          <Crosshair size={12}/> Usar minha localização
        </Button>
      </div>

      <div
        style={{ height }}
        className={`relative rounded-xl overflow-hidden border ${error ? 'border-red-500/60' : 'border-slate-700'}`}>
        <MapContainer
          center={center}
          zoom={value ? 16 : 13}
          scrollWheelZoom
          style={{ height: '100%', width: '100%', background: '#0f172a' }}>
          <TileLayer
            attribution='&copy; OpenStreetMap'
            url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
          />
          <ClickPin value={value} onChange={onChange} onUserMove={onUserMove}/>
          <RecenterControl to={recenter}/>
        </MapContainer>
      </div>

      {error
        ? <p className="text-[11px] text-red-400">{error}</p>
        : <p className="text-[11px] text-slate-500">
            Clique no mapa para definir o local. Você pode arrastar o pin para ajustar.
          </p>
      }
      {value && (
        <p className="text-[11px] font-mono text-slate-500">
          {value.lat.toFixed(6)}, {value.lng.toFixed(6)}
        </p>
      )}
    </div>
  )
}
