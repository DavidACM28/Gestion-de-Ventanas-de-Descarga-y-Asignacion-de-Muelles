package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.CamionDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.UpdateCamionDTO;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.services.CamionService;
import pe.incubadora.backend.utils.AsignarCamionResult;
import pe.incubadora.backend.utils.CreateCamionResult;
import pe.incubadora.backend.utils.DesasignarCamionResult;
import pe.incubadora.backend.utils.UpdateCamionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for truck management endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class CamionController {
    @Autowired
    private CamionService camionService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Creates a new truck.
     *
     * @param camionDTO request payload with truck data
     * @param result validation result populated by Spring
     * @return a creation message or a validation/domain error response
     */
    @PostMapping("/camiones")
    public ResponseEntity<Object> crearCamion(@Valid @RequestBody CamionDTO camionDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            CreateCamionResult resultado = camionService.crearCamion(camionDTO);
            return switch (resultado) {
                case EMPRESA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("EMPRESA_NOT_FOUND", "Empresa no encontrada"));
                case TIPO_CARGA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El tipo de carga no es valido, use: SECA | REFRIGERADA"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creó el camión exitosamente");
            };
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Ya existe un camión con esta placa"));
        }
    }

    /**
     * Updates an existing truck.
     *
     * @param camionDTO partial update payload
     * @param id truck identifier
     * @return the update result translated to an HTTP response
     */
    @PutMapping("/camiones/{id}")
    public ResponseEntity<Object> updateCamiones(@RequestBody UpdateCamionDTO camionDTO, @PathVariable Long id) {
        try {
            UpdateCamionResult resultado = camionService.updateCamion(camionDTO, id);
            return switch (resultado) {
                case CAMION_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("CAMION_NOT_FOUND", "Camión no encontrado"));
                case PLACA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Placa inválida, use formato: ABC-123"));
                case EMPRESA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("EMPRESA_NOT_FOUND", "Empresa no encontrada"));
                case CAMION_ASIGNADO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "No se puede cambiar la empresa, el camión aún tiene un conductor asignado"));
                case TIPO_CARGA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Tipo de carga inválida, use: SECA | REFRIGERADA"));
                case CAPACIDAD_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Capacidad de carga inválida, debe ser mayor a 0"));
                case COLA_ESPERA_ACTIVA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("BUSINESS_RULE_VIOLATION", "No se puede desactivar el camión porque tiene una cola de espera activa"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("El camión se actualizó correctamente");
            };
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "La placa ingresada ya está registrada"));
        }
    }

    /**
     * Assigns a truck to a transport user.
     *
     * @param idCamion truck identifier
     * @param idUsuario user identifier
     * @return assignment result translated to an HTTP response
     */
    @PatchMapping("/camiones/{idCamion}/{idUsuario}/asignar")
    public ResponseEntity<Object> asignarCamion(@PathVariable Long idCamion, @PathVariable Long idUsuario) {
        AsignarCamionResult resultado = camionService.asignarCamion(idCamion, idUsuario);
        return switch (resultado) {
            case CAMION_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("CAMION_NOT_FOUND", "No se encontró el camión"));
            case CAMION_ASIGNADO -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("ASSIGNED_CONFLICT", "El camión ya tiene un usuario asignado"));
            case USUARIO_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("USUARIO_NOT_FOUND", "No se encontró el usuario"));
            case USUARIO_ADMINISTRATIVO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "No se puede asignar camiones a usuarios que no son transportistas"));
            case USUARIO_CON_CAMION -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CAMION_CONFLICT", "El usuario ya tiene un camión asignado"));
            case ASIGNADO ->  ResponseEntity.status(HttpStatus.OK).body("Se asignó el camión correctamente");
        };
    }

    /**
     * Removes the current user assignment from a truck.
     *
     * @param id truck identifier
     * @return unassignment result translated to an HTTP response
     */
    @PatchMapping("/camiones/{id}/desasignar")
    public ResponseEntity<Object> desasignarCamion(@PathVariable Long id) {
        DesasignarCamionResult resultado = camionService.desasignarCamion(id);
        return switch (resultado) {
            case CAMION_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("CAMION_NOT_FOUND", "No se encontró el camión"));
            case USUARIO_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("NOT_ASSIGNED", "El camión no tiene ningún usuario asignado"));
            case DESASIGNADO -> ResponseEntity.status(HttpStatus.OK).body("El camión se desasignó correctamente");
        };
    }

    /**
     * Returns a paginated list of trucks.
     *
     * @param page zero-based page number
     * @return paginated truck data
     */
    @GetMapping("/camiones")
    public ResponseEntity<Object> getCamiones(@RequestParam int page) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        return ResponseEntity.ok().body(camionService.getCamiones(pageable));
    }

    /**
     * Retrieves a single truck by its identifier.
     *
     * @param id truck identifier
     * @return the truck or a not found response
     */
    @GetMapping("/camiones/{id}")
    public ResponseEntity<Object> getCamion(@PathVariable Long id) {
        CamionEntity camion = camionService.getCamion(id);
        if (camion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("CAMION_NOT_FOUND", "Camión no encontrado"));
        }
        return ResponseEntity.ok().body(camion);
    }
}
