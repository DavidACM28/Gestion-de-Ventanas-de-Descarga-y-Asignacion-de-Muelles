package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "camion")
public class CamionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private EmpresaTransportistaEntity empresa;

    @Column(name = "placa", unique = true)
    private String placa;

    private String tipoCarga;
    private BigDecimal capacidadToneladas;
    private boolean activo;

}
