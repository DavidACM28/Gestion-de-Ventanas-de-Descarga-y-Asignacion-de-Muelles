package pe.incubadora.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCamionDTO {

    private Long empresaId;
    private String tipoCarga;
    private BigDecimal capacidadToneladas;
}
