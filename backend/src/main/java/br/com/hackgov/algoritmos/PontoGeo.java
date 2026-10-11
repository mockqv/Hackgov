package br.com.hackgov.algoritmos;

/**
 * Uma ocorrência georreferenciada (vértice do grafo de proximidade).
 *
 * @param id        identificador da solicitação
 * @param latitude  graus decimais (WGS84)
 * @param longitude graus decimais (WGS84)
 * @param categoria tipo de serviço — só ocorrências da mesma categoria se ligam
 */
public record PontoGeo(long id, double latitude, double longitude, String categoria) {}
