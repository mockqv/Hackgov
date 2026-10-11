package br.com.hackgov.algoritmos;

import java.util.Collection;

/**
 * Utilitários geográficos.
 *
 * A regra de duplicidade original (Fase 3/4) usava "graus × 111.000" tanto para
 * latitude quanto para longitude. Isso só vale no Equador: em São Paulo
 * (≈ 23,5° S) um grau de longitude mede ≈ 102 km, não 111 km. Aqui a distância
 * é calculada corretamente — Haversine para distância exata e uma projeção
 * local (equiretangular) para trabalhar no plano, em metros, dentro da KD-Tree.
 */
public final class Geo {

    /** Raio médio da Terra em metros (IUGG). */
    public static final double RAIO_TERRA_M = 6_371_008.8;

    private Geo() {}

    /** Distância ao longo da superfície (fórmula de Haversine), em metros. */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAIO_TERRA_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /**
     * Projeção equiretangular centrada num ponto de referência: converte lat/lon
     * em coordenadas planas (x = leste, y = norte) em metros. Para áreas do
     * tamanho de uma cidade o erro fica bem abaixo de 1% nas distâncias de
     * dezenas de metros usadas no agrupamento.
     */
    public record ProjecaoLocal(double latitudeReferencia, double longitudeReferencia) {

        public static ProjecaoLocal centradaEm(Collection<PontoGeo> pontos) {
            double somaLat = 0;
            double somaLon = 0;
            for (PontoGeo p : pontos) {
                somaLat += p.latitude();
                somaLon += p.longitude();
            }
            int n = Math.max(pontos.size(), 1);
            return new ProjecaoLocal(somaLat / n, somaLon / n);
        }

        public double x(double longitude) {
            return RAIO_TERRA_M * Math.toRadians(longitude - longitudeReferencia)
                    * Math.cos(Math.toRadians(latitudeReferencia));
        }

        public double y(double latitude) {
            return RAIO_TERRA_M * Math.toRadians(latitude - latitudeReferencia);
        }
    }
}
