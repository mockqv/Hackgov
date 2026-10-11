package br.com.hackgov.dto;

import br.com.hackgov.model.Bairro;

/** Bairro para o seletor do formulário de nova solicitação. */
public record BairroResponse(Long id, String nome, String municipio, String uf) {

    public static BairroResponse from(Bairro b) {
        return new BairroResponse(b.getId(), b.getNomeBairro(), b.getMunicipio(), b.getUf());
    }
}
