package br.com.hackgov.demo;

import br.com.hackgov.ia.ClassificadorNaiveBayes;
import br.com.hackgov.ia.CorpusTriagem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeradorDadosDemoTest {

    private static final ClassificadorNaiveBayes IA = ClassificadorNaiveBayes.treinado(CorpusTriagem.carregarPadrao(), 1.0);
    private static final DadosDemo DADOS = GeradorDadosDemo.gerar(GeradorDadosDemo.SEMENTE_PADRAO, IA::classificar);

    @Test
    @DisplayName("mesma semente gera exatamente a mesma massa (resultados reprodutíveis)")
    void deterministico() {
        DadosDemo outra = GeradorDadosDemo.gerar(GeradorDadosDemo.SEMENTE_PADRAO, IA::classificar);
        assertEquals(DADOS.chamados(), outra.chamados());
        assertEquals(DADOS.cidadaos(), outra.cidadaos());
    }

    @Test
    @DisplayName("volume, cadastros e protocolos únicos")
    void volumeECadastros() {
        assertEquals(12, DADOS.bairros().size());
        assertEquals(8, DADOS.servidores().size());
        assertEquals(320, DADOS.cidadaos().size());
        assertTrue(DADOS.chamados().size() > 1_700 && DADOS.chamados().size() < 2_100, "volume: " + DADOS.chamados().size());

        Set<String> protocolos = new HashSet<>();
        DADOS.chamados().forEach(c -> protocolos.add(c.protocolo()));
        assertEquals(DADOS.chamados().size(), protocolos.size());

        // colunas UNIQUE no banco: e-mail e hash do CPF de cada cidadão não podem se repetir
        assertEquals(DADOS.cidadaos().size(), DADOS.cidadaos().stream().map(DadosDemo.Cidadao::email).distinct().count());
        assertEquals(DADOS.cidadaos().size(), DADOS.cidadaos().stream().map(DadosDemo.Cidadao::cpf).distinct().count());
        DADOS.chamados().forEach(c -> {
            assertTrue(c.numero().length() <= 10 && c.cep().length() == 8, "limites de coluna em " + c.protocolo());
            assertTrue(c.descricao().length() <= 500);
        });
    }

    @Test
    @DisplayName("cada chamado respeita a máquina de estados e as regras de datas")
    void consistenciaDosChamados() {
        for (DadosDemo.Chamado c : DADOS.chamados()) {
            assertFalse(c.dataAbertura().toLocalDate().isBefore(GeradorDadosDemo.INICIO));
            assertFalse(c.dataAbertura().isAfter(GeradorDadosDemo.DATA_REFERENCIA));
            assertEquals(c.dataAbertura().toLocalDate().plusDays(GeradorDadosDemo.SLA_DIAS.get(c.codigoTipoEscolhido())),
                    c.dataPrevisao());

            List<DadosDemo.Transicao> h = c.historico();
            String estado = "RECEBIDO";
            var ultimaData = c.dataAbertura();
            for (DadosDemo.Transicao t : h) {
                assertEquals(estado, t.statusAnterior());
                assertTrue(t.dataHora().isAfter(ultimaData), "transição fora de ordem em " + c.protocolo());
                assertFalse(t.dataHora().isAfter(GeradorDadosDemo.DATA_REFERENCIA));
                estado = t.statusNovo();
                ultimaData = t.dataHora();
            }
            assertEquals(estado, c.status());

            if ("CONCLUIDO".equals(c.status())) {
                assertNotNull(c.dataConclusao());
                assertEquals(h.get(h.size() - 1).dataHora(), c.dataConclusao());
            } else {
                assertNull(c.dataConclusao());
                assertNull(c.avaliacao());
            }
            if ("CANCELADO".equals(c.status())) {
                assertNotNull(h.get(h.size() - 1).justificativa());
            }
        }
    }

    @Test
    @DisplayName("a IA acerta o tipo real na maioria das descrições simuladas (texto fora do corpus)")
    void iaGeneralizaParaAMassaSimulada() {
        long comSugestao = DADOS.chamados().stream().filter(c -> c.codigoSugeridoIa() != null).count();
        long acertos = DADOS.chamados().stream()
                .filter(c -> c.codigoTipoReal().equals(c.codigoSugeridoIa())).count();
        assertTrue(comSugestao > DADOS.chamados().size() * 0.95);
        assertTrue(acertos > DADOS.chamados().size() * 0.85, "acertos: " + acertos);
    }
}
