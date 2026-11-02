package br.com.hackgov.integridade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Texto canônico de um evento de auditoria (linha de HISTORICO_STATUS).
 *
 * <p>É dele que sai a folha da árvore de Merkle. Qualquer alteração no banco —
 * trocar o status, a data, o servidor responsável ou a justificativa — muda o
 * texto, muda o hash e invalida a raiz já ancorada. A ordem dos campos e o
 * formato da data são fixos: mudar este método quebraria a verificação dos dias
 * já ancorados, por isso ele tem versão ("v1").
 */
public final class RegistroCanonico {

    public static final String VERSAO = "v1";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private RegistroCanonico() {}

    public static String texto(long idHistorico, String protocolo, String statusAnterior, String statusNovo,
                               LocalDateTime dataHora, Long idServidor, String justificativa) {
        return String.join("|",
                VERSAO,
                String.valueOf(idHistorico),
                protocolo,
                statusAnterior,
                statusNovo,
                dataHora.withNano(0).format(DATA),
                idServidor == null ? "-" : String.valueOf(idServidor),
                // "|" e quebras de linha escapados para que nenhum campo "invada" o próximo
                justificativa == null ? "" : justificativa.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n"));
    }
}
