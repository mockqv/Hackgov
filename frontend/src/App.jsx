import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AppProvider } from './context/AppContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Mapa from './pages/Mapa'
import NovaSolicitacao from './pages/NovaSolicitacao'
import Acompanhar from './pages/Acompanhar'
import Painel from './pages/Painel'
import Dashboard from './pages/Dashboard'

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Layout />}>
            <Route index element={<Navigate to="/mapa" replace />} />
            <Route path="mapa"       element={<Mapa />} />
            <Route path="nova"       element={<NovaSolicitacao />} />
            <Route path="acompanhar" element={<Acompanhar />} />
            <Route path="painel"     element={<Painel />} />
            <Route path="dashboard"  element={<Dashboard />} />
          </Route>
          <Route path="*" element={<Navigate to="/mapa" replace />} />
        </Routes>
      </BrowserRouter>
    </AppProvider>
  )
}
