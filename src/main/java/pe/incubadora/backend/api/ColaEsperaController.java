package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.ColaEsperaDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.entities.ColaEsperaEntity;
import pe.incubadora.backend.services.ColaEsperaService;
import pe.incubadora.backend.utils.CancelarColaEsperaResult;
import pe.incubadora.backend.utils.CreateColaEsperaResult;

import java.time.LocalDate;
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
            case CAMION_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("CAMION_NOT_FOUND", "No se encontró el camión"));
            case FECHA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida. Use formato yyyy-MM-dd"));
            case CARGA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Carga inválida. Use SECA o REFRIGERADA"));
            case CAMION_EN_LISTA -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("COLA_ESPERA_CONFLICT", "El camión ya está en la lista de espera para la fecha solicitada"));
            case NO_COINCIDEN -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El tipo de carga del camión y de la solicitud no coinciden"));
            case COLA_LLENA -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("COLA_LLENA_CONFLICT", "La cola de espera está llena"));
            case CREATED ->
                ResponseEntity.status(HttpStatus.CREATED).body("Se añadió a la cola de espera correctamente");
        };
    }

    @PatchMapping("/cola-espera/{id}/cancelar")
    public ResponseEntity<Object> cancelarColaEspera(Long id) {
        CancelarColaEsperaResult resultado = colaEsperaService.cancelarColaEspera(id);
        return switch (resultado) {
            case COLA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("COLA_NOT_FOUND", "No se encontró la cola de espera"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Solo se pueden cancelar las colas de espera con estado ACTIVA"));
            case CANCELED -> ResponseEntity.status(HttpStatus.OK).body("Se canceló la cola de espera");
        };
    }

    @GetMapping("/cola-espera")
    public ResponseEntity<Object> getColaEspera(
        @RequestParam(required = false) LocalDate fecha, @RequestParam(required = false) String tipoCarga,
        @RequestParam(required = false) String estado, @RequestParam(required = false) Integer prioridad,
        @RequestParam int page, @RequestParam int size, @RequestParam String sort) {

        Page<ColaEsperaEntity> colas =
            colaEsperaService.getColasConFiltros(fecha, tipoCarga, estado, prioridad, page, size, sort);
        return ResponseEntity.status(HttpStatus.OK).body(colas);
    }

}
