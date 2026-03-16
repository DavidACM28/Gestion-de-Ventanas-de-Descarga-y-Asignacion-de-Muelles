package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.incubadora.backend.dtos.ColaEsperaDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.services.ColaEsperaService;
import pe.incubadora.backend.utils.CreateColaEsperaResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ColaEsperaController {
    @Autowired
    private ColaEsperaService colaEsperaService;

    @PostMapping("/cola-espera")
    public ResponseEntity<Object> crearColaEspera(@Valid @RequestBody ColaEsperaDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        CreateColaEsperaResult resultado = colaEsperaService.createColaEspera(dto);
        return switch (resultado) {
            case CAMION_NOT_FOUND ->  ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("CAMION_NOT_FOUND", "No se encontró el camión"));
            case FECHA_INVALIDA ->  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida. Use formato yyyy-MM-dd"));
            case CARGA_INVALIDA ->   ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Carga inválida. Use SECA o REFRIGERADA"));
            case CAMION_EN_LISTA ->  ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("COLA_ESPERA_CONFLICT", "El camión ya está en la lista de espera para la fecha solicitada"));
            case NO_COINCIDEN ->   ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El tipo de carga del camión y de la solicitud no coinciden"));
            case COLA_LLENA -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("COLA_LLENA_CONFLICT", "La cola de espera está llena"));
            case CREATED ->  ResponseEntity.status(HttpStatus.CREATED).body("Se añadió a la cola de espera correctamente");
        };
    }

}
