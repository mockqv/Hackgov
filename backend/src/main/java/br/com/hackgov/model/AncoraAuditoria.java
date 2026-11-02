package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Âncora de um dia de auditoria (Fase 7): a raiz de Merkle dos eventos de
 * HISTORICO_STATUS daquele dia, no momento em que foi gravada na blockchain.
 * O banco guarda só a referência (rede, transação, índice do lote); a prova
 * de verdade está no contrato HackGovRegistro. Uma âncora nunca é alterada.
 */
@Entity
@Table(name = "ANCORA_AUDITORIA")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AncoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ANCORA")
    private Long id;

    @Column(name = "DATA_REFERENCIA", nullable = false, unique = true)
    private LocalDate dataReferencia;

    @Column(name = "RAIZ_MERKLE", nullable = false, length = 66)
    private String raizMerkle;

    @Column(name = "QTD_EVENTOS", nullable = false)
    private Integer qtdEventos;

    @Column(name = "REDE", nullable = false, length = 60)
    private String rede;

    @Column(name = "TX_HASH", nullable = false, length = 66)
    private String txHash;

    @Column(name = "INDICE_LOTE")
    private Long indiceLote;

    @Column(name = "DATA_ANCORAGEM", nullable = false)
    private LocalDateTime dataAncoragem;

    @PrePersist
    protected void prePersist() {
        if (dataAncoragem == null) dataAncoragem = LocalDateTime.now();
    }
}
