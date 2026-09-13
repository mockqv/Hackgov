package br.com.hackgov.audit;

import br.com.hackgov.security.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Registro de auditoria — deliberadamente separado do log técnico da aplicação.
 *
 * Diferença de propósito (ver Parte 5 da documentação da Fase 5):
 *  - LOG TÉCNICO (Slf4j nas classes de service/controller, ex.: log.info(...))
 *    existe para depuração operacional: erro de infraestrutura, stack trace,
 *    latência. É lido por quem opera o sistema e pode ser rotacionado/descartado
 *    livremente.
 *  - REGISTRO DE AUDITORIA (esta classe + a tabela HISTORICO_STATUS) existe para
 *    responsabilização: quem fez o quê, a quem, e quando. Precisa ser íntegro,
 *    não pode ser "limpo" por conveniência, e é o que sustenta a Lei de Acesso à
 *    Informação (LAI) e a prestação de contas exigida de um sistema GovTech.
 *
 * O logger "AUDIT" é nomeado à parte de propósito: em produção, o logback.xml
 * pode rotear esse nome para um appender próprio (arquivo write-once, ou um
 * sink externo tipo SIEM), sem se misturar ao log de aplicação padrão.
 *
 * Esta classe cobre os eventos que NÃO passam por HISTORICO_STATUS (que já
 * audita mudança de status nativamente): consulta a dado pessoal de terceiro,
 * exclusão/inativação de registro do catálogo, e exportação de dados para
 * fora do sistema.
 */
public final class AuditLog {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private AuditLog() {}

    /** Consulta a dado pessoal de terceiro (ex.: SERVIDOR/GESTOR abrindo o perfil de um cidadão). */
    public static void consultaSensivel(String recurso, Long idAlvo) {
        registrar("CONSULTA_SENSIVEL", recurso, idAlvo);
    }

    /** Exclusão lógica ou física de um registro (ex.: inativação de Tipo de Serviço). */
    public static void exclusaoRegistro(String recurso, Long idAlvo) {
        registrar("EXCLUSAO_REGISTRO", recurso, idAlvo);
    }

    /** Exportação de dados para fora do sistema (ex.: CSV de solicitações do cidadão). */
    public static void exportacaoDados(String recurso, Long idAlvo) {
        registrar("EXPORTACAO_DADOS", recurso, idAlvo);
    }

    private static void registrar(String acao, String recurso, Long idAlvo) {
        var principal = AuthUtils.current().orElse(null);
        String ator = principal != null
                ? principal.getKind() + "#" + principal.getId() + "(" + principal.getPerfil() + ")"
                : "ANONIMO";

        MDC.put("acao", acao);
        MDC.put("recurso", recurso);
        MDC.put("alvo", String.valueOf(idAlvo));
        MDC.put("ator", ator);
        try {
            AUDIT.info("acao={} recurso={} alvo={} ator={}", acao, recurso, idAlvo, ator);
        } finally {
            MDC.clear();
        }
    }
}
