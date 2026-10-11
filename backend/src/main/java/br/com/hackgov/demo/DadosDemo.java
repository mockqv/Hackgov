package br.com.hackgov.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Massa de dados de demonstração gerada por {@link GeradorDadosDemo}.
 * Estruturas simples (records) e sem dependência de JPA: o mesmo conjunto é
 * persistido pelo DemoDataSeeder (perfil "demo") e pode ser exportado para
 * conferência das consultas SQL.
 */
public record DadosDemo(
        List<Bairro> bairros,
        List<Servidor> servidores,
        List<Cidadao> cidadaos,
        List<Chamado> chamados) {

    public record Bairro(String nome, double latitude, double longitude, boolean periferia,
                         String prefixoCep, List<String> logradouros) {}

    public record Servidor(String nome, String matricula, String email, String cargo) {}

    public record Cidadao(String nome, String email, String cpf, String telefone, LocalDate dataCadastro) {}

    public record Transicao(String statusAnterior, String statusNovo, LocalDateTime dataHora,
                            int indiceServidor, String justificativa) {}

    public record Avaliacao(int nota, String comentario, LocalDateTime data) {}

    /**
     * Um chamado simulado.
     *
     * @param codigoTipoEscolhido tipo marcado pelo cidadão (vai para SOLICITACAO.ID_TIPO)
     * @param codigoTipoReal      tipo que o texto realmente descreve — conhecido só pelo
     *                            gerador; serve para medir a IA, não é persistido
     * @param codigoSugeridoIa    tipo sugerido pela IA de triagem para a descrição
     */
    public record Chamado(
            String protocolo,
            int indiceCidadao,
            String codigoTipoEscolhido,
            String codigoTipoReal,
            int indiceBairro,
            double latitude,
            double longitude,
            String logradouro,
            String numero,
            String cep,
            String descricao,
            String status,
            LocalDateTime dataAbertura,
            LocalDate dataPrevisao,
            LocalDateTime dataConclusao,
            Integer indiceServidorResponsavel,
            List<Transicao> historico,
            Avaliacao avaliacao,
            String codigoSugeridoIa,
            Double confiancaIa,
            String origem) {}
}
