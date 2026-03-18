package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaDTO {

    @NotNull(message = "El muelle es obligatorio")
    private Long muelleId;

    @NotNull(message = "El camion es obligatorio")
    private Long camionId;

    @NotBlank(message = "La fecha es obligatoria")
    private String fecha;

    @NotBlank(message = "La hora de inicio es obligatoria")
    private String horaInicio;

    @NotNull(message = "La duración en minutos es obligatoria")
    @Min(value = 30, message = "La duración debe ser: 30 | 60 | 90 | 120")
    @Max(value = 120, message = "La duración debe ser: 30 | 60 | 90 | 120")
    private Integer duracionMin;

    @NotNull(message = "El peso estimado en toneladas es obligatorio")
    @DecimalMin(value = "0.1", message = "El peso debe ser mayor a 0")
    private BigDecimal pesoEstimadoToneladas;

    @NotBlank(message = "El tipo de mercadería es obligatorio")
    @Size(min = 3, message = "El tipo de mercadería debe tener 3 caracteres como mínimo")
    private String tipoMercaderia;

    private String nota;
    private Boolean colaEspera  = false;
}
