package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reserva_descarga")
public class ReservaDescargaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "muelle_id")
    private MuelleEntity muelle;

    @ManyToOne
    @JoinColumn(name = "camion_id")
    private CamionEntity camion;

    private LocalDate fecha;
    private LocalTime horaInicio;
    private int duracionMin;
    private BigDecimal pesoEstimadoToneladas;
    private String estado;
    private String tipoMercaderia;
    private String nota;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Integer version;

}
