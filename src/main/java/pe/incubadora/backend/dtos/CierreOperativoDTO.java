package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CierreOperativoDTO {

    @NotNull(message = "El muelle es obligatorio")
    private Long muelleId;

    @NotBlank(message = "La fecha es obligatoria")
    private String fecha;

    @NotBlank(message = "La hora de inicio es obligatoria")
    private String horaInicio;

    @NotBlank(message = "La hora de fin es obligatoria")
    private String horaFin;

    Boolean force = false;
    private String motivo;
    private String tipo;
}
