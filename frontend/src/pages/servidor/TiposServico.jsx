import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { Plus, Pencil, Trash2, RefreshCw, Settings2 } from 'lucide-react'

import useApi from '../../hooks/useApi'
import { tipoServicoApi } from '../../lib/api'
import { TipoServicoSchema } from '../../lib/validation'

import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import Badge from '../../components/ui/Badge'
import Modal from '../../components/ui/Modal'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorState from '../../components/ui/ErrorState'

/**
 * CRUD do catálogo de Tipos de Serviço — exclusivo do perfil GESTOR.
 * Demonstra o consumo completo (Create/Read/Update/Delete) da API RESTful
 * adicionada na Fase 5, com estados de loading/sucesso/erro e validação
 * dupla (zod aqui + Jakarta Bean Validation no backend).
 */
export default function TiposServico() {
  const { data, loading, error, refresh } = useApi(tipoServicoApi.listarTodos, [])
  const items = useMemo(() => data || [], [data])

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing]     = useState(null) // null = criar; objeto = editar
  const [confirming, setConfirming] = useState(null) // item pendente de inativação

  const {
    register, handleSubmit, reset, formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(TipoServicoSchema) })

  const openCreate = () => { setEditing(null); reset({ codigo: '', descricao: '', slaDias: 5 }); setModalOpen(true) }
  const openEdit = (item) => {
    setEditing(item)
    reset({ codigo: item.codigo, descricao: item.descricao, slaDias: item.slaDias })
    setModalOpen(true)
  }

  const onSubmit = async (values) => {
    try {
      if (editing) {
        await tipoServicoApi.atualizar(editing.id, values)
        toast.success('Tipo de serviço atualizado')
      } else {
        await tipoServicoApi.criar(values)
        toast.success('Tipo de serviço criado')
      }
      setModalOpen(false)
      refresh()
    } catch (e) {
      toast.error(e.message || 'Não foi possível salvar')
    }
  }

  const confirmarInativacao = async () => {
    if (!confirming) return
    try {
      await tipoServicoApi.inativar(confirming.id)
      toast.success('Tipo de serviço inativado')
      setConfirming(null)
      refresh()
    } catch (e) {
      toast.error(e.message || 'Não foi possível inativar')
    }
  }

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-bold font-display text-slate-900 flex items-center gap-2">
            <Settings2 size={18}/> Tipos de serviço
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Catálogo administrado pelo Gestor — CRUD completo (POST / PUT / DELETE em /api/tipos-servico)
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={refresh}>
            <RefreshCw size={12} className={loading ? 'animate-spin' : ''}/> Atualizar
          </Button>
          <Button size="sm" onClick={openCreate}>
            <Plus size={14}/> Novo tipo
          </Button>
        </div>
      </header>

      <Card className="overflow-hidden">
        {loading && !data ? (
          <div className="p-5 space-y-3">
            {[1, 2, 3].map(i => <Skeleton key={i} className="h-12 rounded-xl"/>)}
          </div>
        ) : error ? (
          <ErrorState onRetry={refresh}/>
        ) : items.length === 0 ? (
          <EmptyState title="Nenhum tipo de serviço cadastrado"
                      description="Crie o primeiro tipo para liberar a abertura de solicitações."
                      action={<Button size="sm" onClick={openCreate}><Plus size={14}/> Novo tipo</Button>}/>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[10px] uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <th className="px-5 py-3 font-semibold">Código</th>
                <th className="px-5 py-3 font-semibold">Descrição</th>
                <th className="px-5 py-3 font-semibold">SLA (dias)</th>
                <th className="px-5 py-3 font-semibold">Status</th>
                <th className="px-5 py-3 font-semibold text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((t) => (
                <tr key={t.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-5 py-3 font-mono text-xs text-slate-600">{t.codigo}</td>
                  <td className="px-5 py-3 text-slate-800">{t.descricao}</td>
                  <td className="px-5 py-3 text-slate-500">{t.slaDias}</td>
                  <td className="px-5 py-3">
                    <Badge variant={t.ativo ? 'green' : 'slate'}>{t.ativo ? 'Ativo' : 'Inativo'}</Badge>
                  </td>
                  <td className="px-5 py-3">
                    <div className="flex justify-end gap-1.5">
                      <Button variant="ghost" size="sm" onClick={() => openEdit(t)} aria-label="Editar">
                        <Pencil size={13}/>
                      </Button>
                      {t.ativo && (
                        <Button variant="ghost" size="sm" onClick={() => setConfirming(t)} aria-label="Inativar">
                          <Trash2 size={13} className="text-red-500"/>
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {/* Modal criar/editar */}
      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? 'Editar tipo de serviço' : 'Novo tipo de serviço'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)}>Cancelar</Button>
            <Button loading={isSubmitting} onClick={handleSubmit(onSubmit)}>
              {editing ? 'Salvar alterações' : 'Criar'}
            </Button>
          </>
        }>
        <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
          <Input label="Código" required placeholder="EX: PODA-01"
                 error={errors.codigo?.message} {...register('codigo')}/>
          <Input label="Descrição" required placeholder="Ex: Poda de árvore"
                 error={errors.descricao?.message} {...register('descricao')}/>
          <Input label="SLA (dias)" required type="number" min={1} max={365}
                 error={errors.slaDias?.message} {...register('slaDias')}/>
        </form>
      </Modal>

      {/* Confirmação de inativação */}
      <Modal
        open={!!confirming}
        onClose={() => setConfirming(null)}
        title="Inativar tipo de serviço"
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(null)}>Cancelar</Button>
            <Button variant="danger" onClick={confirmarInativacao}>Inativar</Button>
          </>
        }>
        <p className="text-sm text-slate-600">
          Tem certeza que deseja inativar <strong className="text-slate-900">{confirming?.descricao}</strong>?
          Ele deixará de aparecer no formulário de nova solicitação, mas o histórico de
          chamados já abertos com esse tipo é preservado (por isso a exclusão é lógica,
          não física — evita quebrar a integridade referencial com SOLICITACAO).
        </p>
      </Modal>
    </div>
  )
}
