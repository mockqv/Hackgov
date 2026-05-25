package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SERVIDOR")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Servidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SERVIDOR")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 150)
    private String nome;

    @Column(name = "MATRICULA", nullable = false, unique = true, length = 20)
    private String matricula;

    @Column(name = "EMAIL_FUNCIONAL", nullable = false, unique = true, length = 200)
    private String emailFuncional;

    @Column(name = "CARGO", length = 100)
    private String cargo;

    @Column(name = "SENHA_HASH", nullable = false, length = 80)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "PERFIL", nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "ATIVO", nullable = false, length = 1)
    private String ativo = "S";

    @PrePersist
    protected void prePersist() {
        if (ativo == null) ativo = "S";
        if (perfil == null) perfil = Perfil.SERVIDOR;
    }
}
