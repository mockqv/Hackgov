package br.com.hackgov.dto;

import br.com.hackgov.model.Solicitacao;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SolicitacaoDetalheResponse {
    private Long id;
    private String protocolo;
    private String tipoDescricao;
    private String status;
    private String descricao;
    private String caminhoFotoAntes;
    private String caminhoFotoDepois;
    private LocalDateTime dataAbertura;
    private LocalDate dataPrevisao;
    private LocalDateTime dataConclusao;
    private LocalizacaoResponse localizacao;
    private String nomeCidadao;
    private String nomeServidor;
    private Integer slaDias;

    public static SolicitacaoDetalheResponse from(Solicitacao s) {
        return SolicitacaoDetalheResponse.builder()
                .id(s.getId())
                .protocolo(s.getProtocolo())
                .tipoDescricao(s.getTipoServico().getDescricao())
                .slaDias(s.getTipoServico().getSlaDias())
                .status(s.getStatus().name())
                .descricao(s.getDescricao())
                .caminhoFotoAntes(s.getCaminhoFotoAntes())
                .caminhoFotoDepois(s.getCaminhoFotoDepois())
                .dataAbertura(s.getDataAbertura())
                .dataPrevisao(s.getDataPrevisao())
                .dataConclusao(s.getDataConclusao())
                .localizacao(LocalizacaoResponse.from(s.getLocalizacao()))
                .nomeCidadao(s.getCidadao().getNome())
                .nomeServidor(s.getServidor() != null ? s.getServidor().getNome() : null)
                .build();
    }
}
