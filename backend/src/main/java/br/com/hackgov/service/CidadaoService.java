package br.com.hackgov.service;

import br.com.hackgov.audit.AuditLog;
import br.com.hackgov.dto.CidadaoResponse;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.repository.CidadaoRepository;
import br.com.hackgov.security.AuthUtils;
import br.com.hackgov.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CidadaoService {

    private final CidadaoRepository cidadaoRepo;

    public CidadaoResponse buscarPorId(Long id) {
        CidadaoResponse resp = CidadaoResponse.from(
            cidadaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Cidadão não encontrado"))
        );

        // Trilha de auditoria: só registra quando é uma CONSULTA A TERCEIRO
        // (servidor/gestor abrindo o perfil de um cidadão). O próprio cidadão
        // consultando seu próprio perfil não é um evento de auditoria.
        AuthenticatedUser principal = AuthUtils.current().orElse(null);
        boolean consultaAoProprioPerfil = principal != null
                && principal.getKind() == AuthenticatedUser.Kind.CIDADAO
                && principal.getId().equals(id);
        if (!consultaAoProprioPerfil) {
            AuditLog.consultaSensivel("CIDADAO", id);
        }

        return resp;
    }
}
