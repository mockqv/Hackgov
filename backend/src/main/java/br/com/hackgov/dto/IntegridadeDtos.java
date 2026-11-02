package br.com.hackgov.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs da integridade da auditoria (Fase 7). */
public final class IntegridadeDtos {

    private IntegridadeDtos() {}

    /** Situação de um dia: PENDENTE (ainda não ancorado), INTEGRO ou DIVERGENTE. */
    public record LoteDia(
            LocalDate data,
            int eventos,
            String raizAtual,
            long inicioEpoch,
            long fimEpoch,
            String situacao,
            String raizAncorada,
            String rede,
            String txHash,
            Long indiceLote,
            LocalDateTime dataAncoragem,
            String exemploProtocolo) {}

    public record ResumoIntegridade(
            int dias,
            int ancorados,
            int integros,
            int divergentes,
            int pendentes,
            long eventos,
            List<LoteDia> lotes) {}

    public record AncorarRequest(
            @NotBlank @Pattern(regexp = "0x[0-9a-fA-F]{64}", message = "raiz deve ser um hash de 32 bytes em hex")
            String raiz,
            @NotBlank @Size(max = 60) String rede,
            @NotBlank @Pattern(regexp = "0x[0-9a-fA-F]{64}", message = "txHash inválido") String txHash,
            Long indiceLote) {}

    /** Prova de um evento: texto canônico, folha, irmãos até a raiz e a âncora do dia. */
    public record ProvaEvento(
            long idHistorico,
            LocalDateTime dataHora,
            String statusAnterior,
            String statusNovo,
            String folha,
            List<String> prova,
            String raizCalculada,
            String raizAncorada,
            String txHash,
            Long indiceLote,
            boolean provaConfere) {}

    public record ProvaChamado(String protocolo, String hashProtocolo, List<ProvaEvento> eventos) {}
}
