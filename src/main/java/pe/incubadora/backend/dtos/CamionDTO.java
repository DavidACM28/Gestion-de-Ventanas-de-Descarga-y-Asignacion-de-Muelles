package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CamionDTO {

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "^[A-Z]{3}-[0-9]{3}$", message = "La placa debe tener el formato ABC-123")
    private String placa;

    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;

    @NotNull(message = "El tipo de carga es obligatorio")
    private String tipoCarga;

    @NotNull(message = "La capacidad de toneladas es obligatoria")
    @DecimalMin(value = "0.1", message = "La capacidad debe ser mayor a 0")
    private BigDecimal capacidadToneladas;
}
