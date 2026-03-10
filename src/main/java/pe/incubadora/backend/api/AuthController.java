package pe.incubadora.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dtos.*;
import pe.incubadora.backend.dtos.auth.LoginAdministrativoResponseDTO;
import pe.incubadora.backend.dtos.auth.LoginDTO;
import pe.incubadora.backend.dtos.auth.LoginTransportistaResponseDTO;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.security.JwtGenerador;
import pe.incubadora.backend.services.UsuarioService;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtGenerador jwtGenerador;

    @PostMapping("/login")
    @Operation(summary = "Login de usuario", security = {})
    public ResponseEntity<Object> login(@RequestBody LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtGenerador.generarToken(authentication);
        UsuarioEntity usuarioEntity = usuarioService.findByUsername(loginDTO.getUsername());
        if (usuarioEntity == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDTO("VALIDATION_ERROR", "El usuario no existe"));
        }
        if (usuarioEntity.getCamion() == null) {
            return ResponseEntity.status(HttpStatus.OK).body(new LoginAdministrativoResponseDTO(token, "Bearer "));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new LoginTransportistaResponseDTO(token, "Bearer ",
            new UsuarioDTO(
                usuarioEntity.getId(),
                usuarioEntity.getEmpresa(),
                usuarioEntity.getCamion(),
                usuarioEntity.getUsername(),
                usuarioEntity.getRol(),
                usuarioEntity.isActivo()
            )));
    }

}
