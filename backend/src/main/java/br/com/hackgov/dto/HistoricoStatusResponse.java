package br.com.hackgov.dto;

import br.com.hackgov.model.HistoricoStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class HistoricoStatusResponse {
    private String statusAnterior;
    private String statusNovo;
    private LocalDateTime dataHora;
    private String nomeServidor;
    private String justificativa;

    public static HistoricoStatusResponse from(HistoricoStatus h) {
        return HistoricoStatusResponse.builder()
                .statusAnterior(h.getStatusAnterior().name())
                .statusNovo(h.getStatusNovo().name())
                .dataHora(h.getDataHora())
                .nomeServidor(h.getServidor().getNome())
                .justificativa(h.getJustificativa())
                .build();
    }
}
