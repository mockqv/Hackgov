// Ajuste padrão dos ícones do Leaflet com Vite — sem isso eles ficam quebrados.
import L from 'leaflet'
import iconUrl       from 'leaflet/dist/images/marker-icon.png'
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import shadowUrl     from 'leaflet/dist/images/marker-shadow.png'
import 'leaflet/dist/leaflet.css'

// Sobrescreve URLs absolutas que o Leaflet tentava resolver
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({ iconUrl, iconRetinaUrl, shadowUrl })

// Cria um ícone colorido por status. Usa DivIcon (SVG inline) — sem assets externos.
export const statusIcon = (color = '#3b82f6', highlighted = false) =>
  L.divIcon({
    className: 'hackgov-pin',
    html: `
      <span style="
        display:inline-block;width:22px;height:22px;border-radius:50%;
        background:${color};border:3px solid rgba(255,255,255,0.85);
        box-shadow:0 0 0 ${highlighted ? '4px' : '0'} ${color}55,
                   0 4px 10px rgba(0,0,0,0.45);
      "></span>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  })

export default L
