import { useAuth } from '../../contexts/AuthContext'
import PagePlaceholder from '../../components/ui/PagePlaceholder'
export default function Inicio() {
  const { user } = useAuth()
  return <PagePlaceholder title={`Olá, ${user?.nome?.split(' ')[0] || 'cidadão'}`} description="Sua dashboard pessoal será construída adiante." />
}
