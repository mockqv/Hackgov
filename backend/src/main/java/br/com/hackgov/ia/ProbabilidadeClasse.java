package br.com.hackgov.ia;

/** Probabilidade a posteriori de uma classe, P(classe | texto). */
public record ProbabilidadeClasse(String classe, double probabilidade) {}
