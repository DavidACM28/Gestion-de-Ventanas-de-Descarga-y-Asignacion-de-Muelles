package pe.incubadora.backend.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.incubadora.backend.dtos.UsuarioDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginTransportistaResponseDTO {

    private String token;
    private String tokenType;
    private UsuarioDTO usuario;
}
