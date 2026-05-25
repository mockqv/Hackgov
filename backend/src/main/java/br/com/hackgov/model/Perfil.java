package br.com.hackgov.model;

public enum Perfil {
    CIDADAO,
    SERVIDOR,
    GESTOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
