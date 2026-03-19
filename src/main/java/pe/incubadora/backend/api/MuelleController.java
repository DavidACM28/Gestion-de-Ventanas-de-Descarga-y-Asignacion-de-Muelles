package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.MuelleDTO;
import pe.incubadora.backend.entities.MuelleEntity;
import pe.incubadora.backend.services.MuelleService;
import pe.incubadora.backend.utils.UpdateMuelleResult;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for dock management endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class MuelleController {
    @Autowired
    private MuelleService muelleService;

    /**
     * Creates a new dock.
     *
     * @param muelleDTO dock payload
     * @param result validation result populated by Spring
     * @return success or validation response
     */
    @PostMapping("/muelles")
    private ResponseEntity<Object> createMuelle(@Valid @RequestBody MuelleDTO muelleDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            if (muelleService.crearMuelle(muelleDTO)) {
                return ResponseEntity.status(HttpStatus.CREATED).body("Muelle registrado exitosamente");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Tipo de carga inválida, use: SECA | REFRIGERADA | MIXTA"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Ya hay un muelle registrado con este código"));
        }
    }

    /**
     * Updates an existing dock.
     *
     * @param muelleDTO partial update payload
     * @param id dock identifier
     * @return success or validation/domain error response
     */
    @PutMapping("/muelles/{id}")
    private ResponseEntity<Object> updateMuelle(@RequestBody MuelleDTO muelleDTO, @PathVariable Long id) {
        try {
            UpdateMuelleResult resultado = muelleService.actualizarMuelle(muelleDTO, id);
            return switch (resultado) {
                case MUELLE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MUELLE_NOT_FOUND", "Muelle no encontrado"));
                case NOMBRE_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Nombre inválido, el nombre debe tener por lo menos 3 caracteres"));
                case TIPO_CARGA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Tipo de carga inválida, use: SECA | REFRIGERADA | MIXTA"));
                case CAPACIDAD_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Capacidad inválida, la capacidad debe ser mayor a 0"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("El muelle se actualizó con éxito");
            };
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El código ingresado ya está registrado"));
        }
    }

    /**
     * Returns a paginated list of docks.
     *
     * @param page zero-based page number
     * @return paginated dock data
     */
    @GetMapping("/muelles")
    private ResponseEntity<Object> getMuelles(@RequestParam int page) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        return ResponseEntity.status(HttpStatus.OK).body(muelleService.getMuelles(pageable));
    }

    /**
     * Retrieves a dock by identifier.
     *
     * @param id dock identifier
     * @return the dock or a not found response
     */
    @GetMapping("/muelles/{id}")
    private ResponseEntity<Object> getMuelle(@PathVariable Long id) {
        MuelleEntity muelleEntity = muelleService.getMuelle(id);
        if (muelleEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Muelle no encontrado");
        }
        return ResponseEntity.status(HttpStatus.OK).body(muelleEntity);
    }

}
