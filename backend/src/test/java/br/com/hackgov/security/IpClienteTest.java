package br.com.hackgov.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpClienteTest {

    @Test
    @DisplayName("atrás do Nginx (rede interna) usa o X-Real-IP: cada cliente tem seu próprio limite")
    void atrasDoProxy() {
        assertEquals("200.155.10.7", IpCliente.resolver("172.18.0.4", "200.155.10.7"));
        assertEquals("200.155.10.8", IpCliente.resolver("127.0.0.1", "200.155.10.8"));
        assertEquals("2804:14c::1", IpCliente.resolver("10.0.0.2", "2804:14c::1"));
    }

    @Test
    @DisplayName("conexão direta da internet ignora X-Real-IP forjado")
    void cabecalhoForjado() {
        assertEquals("187.10.20.30", IpCliente.resolver("187.10.20.30", "1.2.3.4"));
    }

    @Test
    @DisplayName("sem cabeçalho, ou com valor inválido, fica o IP da conexão")
    void semCabecalhoOuInvalido() {
        assertEquals("172.18.0.4", IpCliente.resolver("172.18.0.4", null));
        assertEquals("172.18.0.4", IpCliente.resolver("172.18.0.4", "999.1.1.1"));
        assertEquals("172.18.0.4", IpCliente.resolver("172.18.0.4", "evil.example.com"));
    }
}
