package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AVALIACAO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_AVALIACAO")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SOLICITACAO", nullable = false, unique = true)
    private Solicitacao solicitacao;

    @Column(name = "NOTA", nullable = false)
    private Integer nota;

    @Column(name = "COMENTARIO", length = 500)
    private String comentario;

    @Column(name = "DATA_AVALIACAO", nullable = false)
    private LocalDateTime dataAvaliacao;

    @PrePersist
    protected void prePersist() {
        if (dataAvaliacao == null) dataAvaliacao = LocalDateTime.now();
    }
}
