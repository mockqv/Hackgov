package br.com.hackgov.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LOCALIZACAO")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Localizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_LOCALIZACAO")
    private Long id;

    @Column(name = "LATITUDE", nullable = false)
    private Double latitude;

    @Column(name = "LONGITUDE", nullable = false)
    private Double longitude;

    @Column(name = "LOGRADOURO", length = 200)
    private String logradouro;

    @Column(name = "NUMERO", length = 10)
    private String numero;

    @Column(name = "COMPLEMENTO", length = 100)
    private String complemento;

    @Column(name = "CEP", length = 8)
    private String cep;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_BAIRRO", nullable = false)
    private Bairro bairro;

    /**
     * Calcula distância aproximada em metros usando fórmula euclidiana simples.
     * Para verificação de duplicata (raio de 10m).
     */
    public double distanciaPara(double lat, double lon) {
        double dlat = this.latitude  - lat;
        double dlng = this.longitude - lon;
        return Math.sqrt(dlat * dlat + dlng * dlng) * 111_000;
    }
}
