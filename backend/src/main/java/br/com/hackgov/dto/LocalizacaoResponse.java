package br.com.hackgov.dto;

import br.com.hackgov.model.Localizacao;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocalizacaoResponse {
    private Double latitude;
    private Double longitude;
    private String logradouro;
    private String numero;
    private String complemento;
    private String cep;
    private String nomeBairro;
    private String municipio;
    private String uf;

    public static LocalizacaoResponse from(Localizacao l) {
        return LocalizacaoResponse.builder()
                .latitude(l.getLatitude())
                .longitude(l.getLongitude())
                .logradouro(l.getLogradouro())
                .numero(l.getNumero())
                .complemento(l.getComplemento())
                .cep(l.getCep())
                .nomeBairro(l.getBairro().getNomeBairro())
                .municipio(l.getBairro().getMunicipio())
                .uf(l.getBairro().getUf())
                .build();
    }
}
