package pe.incubadora.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpresaTransportistaDTO {

    @NotBlank(message = "El ruc es obligatorio")
    @Pattern(regexp = "\\d{11}$", message = "El RUC debe tener 11 dígitos numéricos")
    private String ruc;

    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    @NotBlank(message = "El nombre de contacto es obligatorio")
    private String contactoNombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email es inválido")
    private String email;

    @NotBlank(message = "El telefono de contacto es obligatorio")
    @Pattern(regexp = "\\d{9}$", message = "El número de teléfono debe tener 9 dígitos numéricos")
    private String contactoTelefono;
}
