package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColaEsperaDTO {

    @NotNull(message = "El camión es obligatorio")
    private Long camionId;

    @NotBlank(message = "La fecha es obligatoria")
    private String fecha;

    @NotBlank(message = "El tipo de carga es obligatorio")
    private String tipoCarga;

    private String observacion;
}
