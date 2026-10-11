package br.com.hackgov.ia;

/** Uma frase do corpus de treino com o tipo de serviço correto (rótulo). */
public record ExemploRotulado(String texto, String classe) {

    public ExemploRotulado {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("texto vazio");
        if (classe == null || classe.isBlank()) throw new IllegalArgumentException("classe vazia");
        classe = classe.trim().toUpperCase();
    }
}
