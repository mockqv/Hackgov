export const PERFIL = {
  CIDADAO:  'CIDADAO',
  SERVIDOR: 'SERVIDOR',
  GESTOR:   'GESTOR',
}

/** Página inicial padrão por perfil (após login). */
export const homeForRole = (perfil) => {
  switch (perfil) {
    case PERFIL.SERVIDOR:
    case PERFIL.GESTOR:
      return '/painel'
    case PERFIL.CIDADAO:
    default:
      return '/inicio'
  }
}
