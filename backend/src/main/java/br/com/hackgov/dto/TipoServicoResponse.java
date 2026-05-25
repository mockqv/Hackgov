package br.com.hackgov.dto;

import br.com.hackgov.model.TipoServico;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TipoServicoResponse {
    private Long id;
    private String codigo;
    private String descricao;
    private Integer slaDias;

    public static TipoServicoResponse from(TipoServico t) {
        return TipoServicoResponse.builder()
                .id(t.getId())
                .codigo(t.getCodigo())
                .descricao(t.getDescricao())
                .slaDias(t.getSlaDias())
                .build();
    }
}
