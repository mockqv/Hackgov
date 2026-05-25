import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import { statusIcon } from './leafletSetup'
import { STATUS_DOT } from '../ui/StatusBadge'

const DEFAULT_CENTER = [-23.55052, -46.633308] // São Paulo
const DEFAULT_ZOOM = 13

/**
 * Mapa somente-leitura com pins de solicitações.
 * @param {{ items: Array, onSelect?: Function, selectedId?: number, height?: string }} props
 */
export default function CityMap({ items = [], onSelect, selectedId, height = '100%' }) {
  // Centro: média das coordenadas válidas, senão São Paulo
  const validos = items.filter(i => i.latitude != null && i.longitude != null)
  const center = validos.length > 0
    ? [
        validos.reduce((s, i) => s + i.latitude,  0) / validos.length,
        validos.reduce((s, i) => s + i.longitude, 0) / validos.length,
      ]
    : DEFAULT_CENTER

  return (
    <div style={{ height, width: '100%' }} className="relative rounded-2xl overflow-hidden border border-slate-800">
      <MapContainer
        center={center}
        zoom={DEFAULT_ZOOM}
        scrollWheelZoom
        style={{ height: '100%', width: '100%', background: '#0f172a' }}>
        <TileLayer
          attribution='&copy; OpenStreetMap'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        />
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
