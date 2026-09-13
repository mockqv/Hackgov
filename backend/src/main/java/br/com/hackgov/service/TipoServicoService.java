// ── TipoServicoService ──────────────────────────────────────────
package br.com.hackgov.service;

import br.com.hackgov.audit.AuditLog;
import br.com.hackgov.dto.TipoServicoRequest;
import br.com.hackgov.dto.TipoServicoResponse;
import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.model.TipoServico;
import br.com.hackgov.repository.TipoServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD do catálogo de Tipos de Serviço. Leitura da lista ativa é pública
 * (usada no formulário de Nova Solicitação); criação, edição e inativação
 * são restritas ao perfil GESTOR (ver @PreAuthorize no controller).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TipoServicoService {

    private final TipoServicoRepository repo;

    // ── READ ──────────────────────────────────────────────────
    public List<TipoServicoResponse> listarAtivos() {
        return repo.findByAtivoOrderByDescricao("S")
                .stream().map(TipoServicoResponse::from).toList();
    }

    public List<TipoServicoResponse> listarTodos() {
        return repo.findAllByOrderByDescricao()
                .stream().map(TipoServicoResponse::from).toList();
    }

    public TipoServicoResponse buscarPorId(Long id) {
        return TipoServicoResponse.from(buscarEntidade(id));
    }

    // ── CREATE ────────────────────────────────────────────────
    @Transactional
    public TipoServicoResponse criar(TipoServicoRequest req) {
        if (repo.existsByCodigoIgnoreCase(req.getCodigo())) {
            throw new BusinessException("Já existe um tipo de serviço com o código '" + req.getCodigo() + "'");
        }
        TipoServico tipo = TipoServico.builder()
                .codigo(req.getCodigo().trim().toUpperCase())
                .descricao(req.getDescricao().trim())
                .slaDias(req.getSlaDias())
                .ativo("S")
                .build();
        TipoServico salvo = repo.save(tipo);
        log.info("Tipo de serviço criado: {} ({})", salvo.getCodigo(), salvo.getId());
        return TipoServicoResponse.from(salvo);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public TipoServicoResponse atualizar(Long id, TipoServicoRequest req) {
        TipoServico tipo = buscarEntidade(id);
        if (repo.existsByCodigoIgnoreCaseAndIdNot(req.getCodigo(), id)) {
            throw new BusinessException("Já existe outro tipo de serviço com o código '" + req.getCodigo() + "'");
        }
        tipo.setCodigo(req.getCodigo().trim().toUpperCase());
        tipo.setDescricao(req.getDescricao().trim());
        tipo.setSlaDias(req.getSlaDias());
        TipoServico salvo = repo.save(tipo);
        log.info("Tipo de serviço atualizado: {} ({})", salvo.getCodigo(), salvo.getId());
        return TipoServicoResponse.from(salvo);
    }

    // ── DELETE (soft) ─────────────────────────────────────────
    // Exclusão física é evitada de propósito: TIPO_SERVICO é referenciado por
    // SOLICITACAO (FK). Inativar preserva o histórico e a integridade referencial,
    // apenas removendo o tipo das opções oferecidas a novos chamados.
    @Transactional
    public void inativar(Long id) {
        TipoServico tipo = buscarEntidade(id);
        if ("N".equals(tipo.getAtivo())) {
            throw new BusinessException("Tipo de serviço já está inativo");
        }
        tipo.setAtivo("N");
        repo.save(tipo);
        log.info("Tipo de serviço inativado: {} ({})", tipo.getCodigo(), tipo.getId());
        AuditLog.exclusaoRegistro("TIPO_SERVICO", tipo.getId());
    }

    private TipoServico buscarEntidade(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de serviço não encontrado"));
    }
}
