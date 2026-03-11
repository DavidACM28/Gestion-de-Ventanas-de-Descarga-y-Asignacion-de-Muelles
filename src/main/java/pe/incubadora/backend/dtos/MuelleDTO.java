package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MuelleDTO {

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, message = "El nombre debe tener por lo menos 3 caracteres")
    private String nombre;

    @NotBlank(message = "El tipo de carga permitida es obligatorio")
    private String tipoCargaPermitida;

    @NotNull(message = "La capacidad de toneladas es obligatoria")
    @DecimalMin(value = "0.1", message = "La capacidad debe ser mayor a 0")
    private BigDecimal capacidadToneladas;

}
