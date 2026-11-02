package br.com.hackgov.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Descobre o IP real do cliente para o rate limiting (Fase 7, Parte 2 — redes).
 *
 * <p>Atrás do Nginx, {@code getRemoteAddr()} é sempre o IP do contêiner do proxy.
 * Até a Fase 6, isso fazia todos os usuários dividirem o mesmo balde de 5 logins
 * por minuto: um atacante errando a senha 5 vezes bloqueava o login de todo mundo
 * (ataque à disponibilidade). O Nginx informa o IP de origem em {@code X-Real-IP};
 * mas esse cabeçalho só é confiável quando a conexão vem da rede interna (o próprio
 * proxy). Vindo da internet, qualquer um poderia forjá-lo para fugir do limite.
 */
public final class IpCliente {

    private IpCliente() {}

    public static String resolver(String remoteAddr, String xRealIp) {
        if (xRealIp == null || xRealIp.isBlank() || !redeInterna(remoteAddr)) {
            return remoteAddr;
        }
        String candidato = xRealIp.trim();
        return ipValido(candidato) ? candidato : remoteAddr;
    }

    /** Loopback ou faixa privada (10/8, 172.16/12, 192.168/16, fc00::/7): onde roda o proxy. */
    static boolean redeInterna(String ip) {
        if (!ipValido(ip)) return false;
        try {
            InetAddress a = InetAddress.getByName(ip);
            if (a.isLoopbackAddress() || a.isSiteLocalAddress()) return true;
            byte[] b = a.getAddress();
            return b.length == 16 && (b[0] & 0xFE) == 0xFC;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /** Aceita só literais IPv4/IPv6 (nunca nomes, para não disparar consulta DNS). */
    static boolean ipValido(String ip) {
        if (ip == null || ip.isEmpty() || ip.length() > 45) return false;
        if (ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            for (String p : ip.split("\\.")) if (Integer.parseInt(p) > 255) return false;
            return true;
        }
        return ip.contains(":") && ip.matches("[0-9a-fA-F:.]+");
    }
}
