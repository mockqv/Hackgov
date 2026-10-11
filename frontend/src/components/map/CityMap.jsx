import { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L, { statusIcon } from './leafletSetup'
import { STATUS_DOT } from '../ui/StatusBadge'

const DEFAULT_CENTER = [-23.55052, -46.633308] // São Paulo
const DEFAULT_ZOOM = 13

/**
 * Mapa somente-leitura com pins de solicitações.
 * @param {{ items: Array, onSelect?: Function, selectedId?: number, height?: string, zoom?: number,
 *           ajustarAosPontos?: boolean }} props
 */
export default function CityMap({ items = [], onSelect, selectedId, height = '100%', zoom = DEFAULT_ZOOM, ajustarAosPontos = false }) {
  // Centro: média das coordenadas válidas, senão São Paulo
  const validos = items.filter(i => i.latitude != null && i.longitude != null)
  const center = validos.length > 0
    ? [
        validos.reduce((s, i) => s + i.latitude,  0) / validos.length,
        validos.reduce((s, i) => s + i.longitude, 0) / validos.length,
      ]
    : DEFAULT_CENTER

  return (
    <div style={{ height, width: '100%' }} className="relative rounded-2xl overflow-hidden border border-slate-200">
      <MapContainer
        center={center}
        zoom={zoom}
        scrollWheelZoom
        style={{ height: '100%', width: '100%', background: '#f1f5f9' }}>
        <TileLayer
          attribution='&copy; OpenStreetMap'
          url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
        />
        {ajustarAosPontos && <EnquadrarPontos pontos={validos}/>}
        {validos.map(item => (
          <Marker
            key={item.id}
            position={[item.latitude, item.longitude]}
            icon={statusIcon(STATUS_DOT[item.status] || '#64748b', selectedId === item.id)}
            eventHandlers={{ click: () => onSelect?.(item) }}>
            <Popup>
              <div className="text-xs">
                <div className="font-mono text-slate-500">{item.protocolo}</div>
                <div className="font-bold text-slate-800 mt-0.5">{item.tipoDescricao}</div>
                <div className="text-slate-600">{item.logradouro || item.nomeBairro || ''}</div>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}

/** Ajusta o zoom/centro para que todos os pontos apareçam (ex.: pontos críticos espalhados pela cidade). */
function EnquadrarPontos({ pontos }) {
  const map = useMap()
  const chave = pontos.map(p => `${p.latitude.toFixed(5)},${p.longitude.toFixed(5)}`).join('|')
  useEffect(() => {
    if (pontos.length === 0) return
    if (pontos.length === 1) {
      map.setView([pontos[0].latitude, pontos[0].longitude], 15)
      return
    }
    map.fitBounds(L.latLngBounds(pontos.map(p => [p.latitude, p.longitude])), { padding: [28, 28], maxZoom: 15 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, chave])
  return null
}
