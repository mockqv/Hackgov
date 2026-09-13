package br.com.hackgov.dto;

import br.com.hackgov.model.Solicitacao;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
public class SolicitacaoResumoResponse {
    private Long id;
    private String protocolo;
    private String tipoDescricao;
    private String status;
    private String logradouro;
    private String nomeBairro;
    private Double latitude;
    private Double longitude;
    private LocalDateTime dataAbertura;
    private LocalDate dataPrevisao;
    /** Dias até o vencimento do SLA (negativo = já vencido). Usado para priorizar a fila de atendimento. */
    private Long diasParaSla;

    public static SolicitacaoResumoResponse from(Solicitacao s) {
        return SolicitacaoResumoResponse.builder()
                .id(s.getId())
                .protocolo(s.getProtocolo())
                .tipoDescricao(s.getTipoServico().getDescricao())
                .status(s.getStatus().name())
                .logradouro(s.getLocalizacao().getLogradouro())
                .nomeBairro(s.getLocalizacao().getBairro().getNomeBairro())
                .latitude(s.getLocalizacao().getLatitude())
                .longitude(s.getLocalizacao().getLongitude())
                .dataAbertura(s.getDataAbertura())
                .dataPrevisao(s.getDataPrevisao())
                .diasParaSla(s.getDataPrevisao() != null
                        ? ChronoUnit.DAYS.between(LocalDate.now(), s.getDataPrevisao())
                        : null)
                .build();
    }
}
