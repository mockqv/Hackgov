package br.com.hackgov.exception;

// ── EXCEÇÕES DE NEGÓCIO ────────────────────────────────────────
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
