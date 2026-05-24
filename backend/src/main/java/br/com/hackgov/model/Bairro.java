package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BAIRRO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bairro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_BAIRRO")
    private Long id;

    @Column(name = "NOME_BAIRRO", nullable = false, length = 100)
    private String nomeBairro;

    @Column(name = "MUNICIPIO", nullable = false, length = 100)
    private String municipio;

    @Column(name = "UF", nullable = false, length = 2)
    private String uf;
}
