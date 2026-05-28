import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

import PrivateRoute    from './PrivateRoute'
import PublicOnlyRoute from './PublicOnlyRoute'
import RoleRoute       from './RoleRoute'
import { PERFIL }      from './roles'

// Layouts
import AppLayout    from '../components/layout/AppLayout'
import PublicLayout from '../components/layout/PublicLayout'
import SmartLayout  from '../components/layout/SmartLayout'

// Páginas públicas
import Login        from '../pages/public/Login'
import Register     from '../pages/public/Register'
import MapaPublico  from '../pages/public/MapaPublico'

// Páginas do cidadão
import Inicio              from '../pages/cidadao/Inicio'
import NovaSolicitacao     from '../pages/cidadao/NovaSolicitacao'
import Acompanhar          from '../pages/cidadao/Acompanhar'
import MinhasSolicitacoes  from '../pages/cidadao/MinhasSolicitacoes'

// Páginas do servidor
import Painel    from '../pages/servidor/Painel'
import Dashboard from '../pages/servidor/Dashboard'

// Erros
import Forbidden from '../pages/erro/Forbidden'
import NotFound  from '../pages/erro/NotFound'

// Redirect raiz baseado em login + perfil
import RootRedirect from './RootRedirect'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* raiz dinâmica */}
        <Route path="/" element={<RootRedirect />} />

        {/* ─── MAPA PÚBLICO — layout adaptativo (sidebar se logado, header público se visitante) ─── */}
        <Route element={<SmartLayout />}>
          <Route path="mapa-publico" element={<MapaPublico />} />
        </Route>

        {/* ─── AUTENTICAÇÃO (sempre PublicLayout) ─── */}
        <Route element={<PublicLayout />}>
          <Route path="login"    element={<PublicOnlyRoute><Login    /></PublicOnlyRoute>} />
          <Route path="cadastro" element={<PublicOnlyRoute><Register /></PublicOnlyRoute>} />
        </Route>

        {/* ─── PRIVADAS — CIDADÃO (AppLayout) ─── */}
        <Route element={<PrivateRoute><AppLayout /></PrivateRoute>}>
          <Route path="inicio"               element={<RoleRoute roles={[PERFIL.CIDADAO]}><Inicio              /></RoleRoute>} />
          <Route path="nova-solicitacao"     element={<RoleRoute roles={[PERFIL.CIDADAO]}><NovaSolicitacao     /></RoleRoute>} />
          <Route path="acompanhar"           element={<RoleRoute roles={[PERFIL.CIDADAO]}><Acompanhar          /></RoleRoute>} />
          <Route path="acompanhar/:protocolo" element={<RoleRoute roles={[PERFIL.CIDADAO]}><Acompanhar         /></RoleRoute>} />
          <Route path="minhas-solicitacoes"  element={<RoleRoute roles={[PERFIL.CIDADAO]}><MinhasSolicitacoes  /></RoleRoute>} />

          {/* ─── PRIVADAS — SERVIDOR/GESTOR ─── */}
          <Route path="painel"    element={<RoleRoute roles={[PERFIL.SERVIDOR, PERFIL.GESTOR]}><Painel    /></RoleRoute>} />
          <Route path="dashboard" element={<RoleRoute roles={[PERFIL.SERVIDOR, PERFIL.GESTOR]}><Dashboard /></RoleRoute>} />
        </Route>

        {/* ─── ERROS ─── */}
        <Route path="403" element={<Forbidden />} />
        <Route path="404" element={<NotFound  />} />
        <Route path="*"   element={<Navigate to="/404" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
