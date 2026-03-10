package pe.incubadora.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.UsuarioDTO;
import pe.incubadora.backend.dtos.auth.LoginAdministrativoResponseDTO;
import pe.incubadora.backend.dtos.auth.LoginDTO;
import pe.incubadora.backend.dtos.auth.LoginTransportistaResponseDTO;
import pe.incubadora.backend.dtos.auth.RegisterDTO;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.security.JwtGenerador;
import pe.incubadora.backend.services.UsuarioService;
import pe.incubadora.backend.utils.RegisterUsuarioResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
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

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody RegisterDTO registerDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try{
            RegisterUsuarioResult resultado = usuarioService.register(registerDTO);
            return switch (resultado) {
                case ROL_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("ROL_NOT_FOUND", "El rol no existe, use: TRANSPORTISTA | OPERADOR | ADMIN"));
                case ROL_TRANSPORTISTA_CONFLICT -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El transportista debe tener un camión y una empresa asignados"));
                case EMPRESA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("EMPRESA_NOT_FOUND", "La empresa no existe"));
                case CAMION_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("CAMION_NOT_FOUND", "El camión no existe"));
                case CAMION_CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("CAMION_CONFLICT", "El camión ya está asignado a un transportista"));
                case CAMION_NOT_MATCH -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La empresa y el camión no coinciden"));
                case ROL_ADMINISTRATIVO_CONFLICT -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Un rol administrativo no debe tener camión ni empresa"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("El usuario se creó con éxito");
            };
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El nombre de usuario ya existe"));
        }
    }

}






















