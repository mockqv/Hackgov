package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "HISTORICO_STATUS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_HISTORICO")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SOLICITACAO", nullable = false)
    private Solicitacao solicitacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SERVIDOR", nullable = false)
    private Servidor servidor;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_ANTERIOR", nullable = false, length = 20)
    private StatusSolicitacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_NOVO", nullable = false, length = 20)
    private StatusSolicitacao statusNovo;

    @Column(name = "DATA_HORA", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "JUSTIFICATIVA", length = 500)
    private String justificativa;

    @PrePersist
    protected void prePersist() {
        if (dataHora == null) dataHora = LocalDateTime.now();
    }
}
