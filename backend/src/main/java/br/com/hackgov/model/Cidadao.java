package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "CIDADAO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cidadao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CIDADAO")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 150)
    private String nome;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 200)
    private String email;

    // Armazenado como hash SHA-256 (LGPD)
    @Column(name = "CPF_HASH", nullable = false, unique = true, length = 64)
    private String cpfHash;

    @Column(name = "TELEFONE", length = 20)
    private String telefone;

    @Column(name = "DATA_CADASTRO", nullable = false)
    private LocalDate dataCadastro;

    @Column(name = "ATIVO", nullable = false, length = 1)
    private String ativo = "S";

    @PrePersist
    protected void prePersist() {
        if (dataCadastro == null) dataCadastro = LocalDate.now();
        if (ativo == null) ativo = "S";
    }
}
