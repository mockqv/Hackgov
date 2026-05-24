package br.com.hackgov.dto;

import br.com.hackgov.model.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ════════════════════════════════════════════════════════════════
// SOLICITAÇÃO — Request
// ════════════════════════════════════════════════════════════════
@Data
public class SolicitacaoRequest {

    @NotNull(message = "ID do cidadão é obrigatório")
    private Long cidadaoId;

    @NotNull(message = "ID do tipo de serviço é obrigatório")
    private Long tipoServicoId;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 10, max = 500, message = "Descrição deve ter entre 10 e 500 caracteres")
    private String descricao;

    @NotNull(message = "Latitude é obrigatória")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    private String logradouro;
    private String numero;
    private String complemento;
    private String cep;

    @NotNull(message = "ID do bairro é obrigatório")
    private Long bairroId;

    private String caminhoFotoAntes;
}

// ════════════════════════════════════════════════════════════════
// SOLICITAÇÃO — Response (resumo para lista/mapa)
// ════════════════════════════════════════════════════════════════
@Data @Builder
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
                .build();
    }
}

// ════════════════════════════════════════════════════════════════
// SOLICITAÇÃO — Response (detalhe completo)
// ════════════════════════════════════════════════════════════════
@Data @Builder
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

// ════════════════════════════════════════════════════════════════
// LOCALIZAÇÃO — Response
// ════════════════════════════════════════════════════════════════
@Data @Builder
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

// ════════════════════════════════════════════════════════════════
// ATUALIZAR STATUS — Request
// ════════════════════════════════════════════════════════════════
@Data
public class AtualizarStatusRequest {

    @NotNull(message = "ID do servidor é obrigatório")
    private Long servidorId;

    @NotBlank(message = "Novo status é obrigatório")
    private String novoStatus;

    @Size(max = 500)
    private String justificativa;

    private String caminhoFotoDepois;
}

// ════════════════════════════════════════════════════════════════
// HISTÓRICO STATUS — Response
// ════════════════════════════════════════════════════════════════
@Data @Builder
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

// ════════════════════════════════════════════════════════════════
// AVALIAÇÃO — Request
// ════════════════════════════════════════════════════════════════
@Data
public class AvaliacaoRequest {

    @NotNull
    @Min(1) @Max(5)
    private Integer nota;

    @Size(max = 500)
    private String comentario;
}

// ════════════════════════════════════════════════════════════════
// CIDADÃO — Request
// ════════════════════════════════════════════════════════════════
@Data
public class CidadaoRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150)
    private String nome;

    @NotBlank @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    private String cpf; // será convertido para hash

    @Size(max = 20)
    private String telefone;
}

// ════════════════════════════════════════════════════════════════
// CIDADÃO — Response
// ════════════════════════════════════════════════════════════════
@Data @Builder
public class CidadaoResponse {
    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private LocalDate dataCadastro;

    public static CidadaoResponse from(Cidadao c) {
        return CidadaoResponse.builder()
                .id(c.getId())
                .nome(c.getNome())
                .email(c.getEmail())
                .telefone(c.getTelefone())
                .dataCadastro(c.getDataCadastro())
                .build();
    }
}

// ════════════════════════════════════════════════════════════════
// DASHBOARD — Response
// ════════════════════════════════════════════════════════════════
@Data @Builder
public class DashboardResponse {
    private long totalSolicitacoes;
    private long abertas;
    private long emAndamento;
    private long concluidas;
    private long canceladas;
    private double taxaConclusao;
    private Double notaMediaAvaliacao;
    private List<CountPorTipo> countPorTipo;
    private List<TempoMedioTipo> tempoMedioPorTipo;

    @Data @Builder
    public static class CountPorTipo {
        private String tipo;
        private Long quantidade;
    }

    @Data @Builder
    public static class TempoMedioTipo {
        private String tipo;
        private Double mediaDias;
    }
}

// ════════════════════════════════════════════════════════════════
// TIPO SERVIÇO — Response
// ════════════════════════════════════════════════════════════════
@Data @Builder
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

// ════════════════════════════════════════════════════════════════
// API RESPONSE — wrapper padrão
// ════════════════════════════════════════════════════════════════
@Data @Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().success(false).message(message).build();
    }
}
