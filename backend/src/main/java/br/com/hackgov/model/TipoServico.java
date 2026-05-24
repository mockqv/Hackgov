package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TIPO_SERVICO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TIPO")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "DESCRICAO", nullable = false, length = 100)
    private String descricao;

    @Column(name = "SLA_DIAS", nullable = false)
    private Integer slaDias;

    @Column(name = "ATIVO", nullable = false, length = 1)
    private String ativo = "S";
}
