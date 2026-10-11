package br.com.hackgov.dto;

import br.com.hackgov.model.Solicitacao;
import br.com.hackgov.model.TipoServico;
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
    private String codigoTipo;
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
    /** Sugestão da IA de triagem gravada na abertura (Fase 6) — o painel destaca quando diverge do tipo escolhido. */
    private String codigoTipoSugeridoIa;
    private String tipoSugeridoIa;
    private Double confiancaIa;

    public static SolicitacaoResumoResponse from(Solicitacao s) {
        TipoServico sugerido = s.getTipoSugeridoIa();
        return SolicitacaoResumoResponse.builder()
                .id(s.getId())
                .protocolo(s.getProtocolo())
                .codigoTipo(s.getTipoServico().getCodigo())
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
                .codigoTipoSugeridoIa(sugerido != null ? sugerido.getCodigo() : null)
                .tipoSugeridoIa(sugerido != null ? sugerido.getDescricao() : null)
                .confiancaIa(s.getConfiancaIa())
                .build();
    }
}
