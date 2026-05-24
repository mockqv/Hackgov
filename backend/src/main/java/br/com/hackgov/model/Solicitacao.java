package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "SOLICITACAO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SOLICITACAO")
    private Long id;

    @Column(name = "PROTOCOLO", nullable = false, unique = true, length = 30)
    private String protocolo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_CIDADAO", nullable = false)
    private Cidadao cidadao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_TIPO", nullable = false)
    private TipoServico tipoServico;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "ID_LOCALIZACAO", nullable = false)
    private Localizacao localizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SERVIDOR")
    private Servidor servidor;

    @Column(name = "DESCRICAO", nullable = false, length = 500)
    private String descricao;

    @Column(name = "CAMINHO_FOTO_ANTES", length = 300)
    private String caminhoFotoAntes;

    @Column(name = "CAMINHO_FOTO_DEPOIS", length = 300)
    private String caminhoFotoDepois;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private StatusSolicitacao status;

    @Column(name = "DATA_ABERTURA", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "DATA_PREVISAO")
    private LocalDate dataPrevisao;

    @Column(name = "DATA_CONCLUSAO")
    private LocalDateTime dataConclusao;

    @PrePersist
    protected void prePersist() {
        if (protocolo == null) {
            String ano = String.valueOf(LocalDateTime.now().getYear());
            String uuid = UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase();
            this.protocolo = "ZEL-" + ano + "-" + uuid;
        }
        if (dataAbertura == null) dataAbertura = LocalDateTime.now();
        if (status == null) status = StatusSolicitacao.RECEBIDO;
    }

    public void atualizarStatus(StatusSolicitacao novoStatus) {
        this.status = novoStatus;
        if (novoStatus == StatusSolicitacao.CONCLUIDO) {
            this.dataConclusao = LocalDateTime.now();
        }
    }
}
