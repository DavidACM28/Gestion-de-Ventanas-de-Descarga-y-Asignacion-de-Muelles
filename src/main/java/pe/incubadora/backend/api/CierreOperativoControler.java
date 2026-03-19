package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.incubadora.backend.dtos.CierreOperativoDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.entities.CierreOperativoEntity;
import pe.incubadora.backend.services.CierreOperativoService;
import pe.incubadora.backend.utils.CreateCierreOperativoResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Exposes operational closure endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class CierreOperativoControler {
    @Autowired
    private CierreOperativoService cierreOperativoService;

    /**
     * Handles invalid request parameter types used in filter endpoints.
     *
     * @return a standardized validation error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatchException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    /**
     * Handles invalid enum or parsing arguments coming from request parameters.
     *
     * @return a standardized validation error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    /**
     * Handles missing required pagination parameters.
     *
     * @return a standardized validation error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameterException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Los parámetros: size, page, y sort, son obligatorios"));
    }

    /**
     * Handles invalid date filters in requests.
     *
     * @return a standardized validation error response
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Object> handleDateTimeParseException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Fecha invalida. Use formato yyyy-MM-dd"));
    }

    /**
     * Creates a new operational closure.
     *
     * @param cierreOperativoDTO closure payload
     * @param result validation result populated by Spring
     * @return a creation response or a domain-specific validation error
     */
    @PostMapping("/cierres")
    public ResponseEntity<Object> crearCierre(@Valid @RequestBody CierreOperativoDTO cierreOperativoDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        CreateCierreOperativoResult resultado = cierreOperativoService.createCierreOperativo(cierreOperativoDTO);
        return switch (resultado) {
            case MUELLE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("MUELLE_NOT_FOUND", "No se encontró el muelle"));
            case FECHA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida. Use formato yyyy-MM-dd"));
            case HORA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Hora inválida. Use formato HH:mm"));
            case TIPO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Tipo inválido. Use: LIMPIEZA, MANTENIMIENTO, INSPECCION o EMERGENCIA"));
            case CIERRE_CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("CIERRE_CONFLICT", "Ya existe un cierre dentro de este rango de hora"));
            case EN_DESCARGA -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                new ErrorResponseDTO("BUSINESS_RULE_VIOLATION", "Hay una reserva en descarga en este rango de hora"));
            case RESERVA_CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("RESERVA_CONFLICT", "Hay una reserva en este rango de hora"));
            case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body("Se creó el cierre operativo");
        };
    }

    /**
     * Retrieves operational closures using optional filters and pagination.
     *
     * @param muelleId dock identifier filter
     * @param fechaDesde lower date bound
     * @param fechaHasta upper date bound
     * @param tipo closure type filter
     * @param page zero-based page number
     * @param size page size
     * @param sort sort direction keyword
     * @return a paginated list of closures
     */
    @GetMapping("/cierres")
    public ResponseEntity<Object> getCierreOperativos(
        @RequestParam(required = false) Long muelleId, @RequestParam(required = false) String fechaDesde,
        @RequestParam(required = false) String fechaHasta, @RequestParam(required = false) String tipo,
        @RequestParam int page,  @RequestParam int size, @RequestParam String sort) {

        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde, DateTimeFormatter.ISO_DATE) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta, DateTimeFormatter.ISO_DATE) : null;
        if (desde != null && hasta != null && !desde.isBefore(hasta) && !desde.isEqual(hasta)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "La fecha límite de búsqueda no puede ser anterior a la fecha de inicio de búsqueda"));
        }

        Page<CierreOperativoEntity> reservas =
            cierreOperativoService.getCierresConFiltros(muelleId, desde, hasta, tipo, page, size, sort);
        return ResponseEntity.status(HttpStatus.OK).body(reservas);
    }

    /**
     * Deletes an operational closure by identifier.
     *
     * @param id closure identifier
     * @return success or not found response
     */
    @DeleteMapping("/cierres/{id}")
    public ResponseEntity<Object> eliminarCierreOperativo(@PathVariable Long id) {
        if (cierreOperativoService.deleteCierreOperativo(id)) {
            return ResponseEntity.ok().body("Se elimino el cierre operativo");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErrorResponseDTO("CIERRE_NOT_FOUND", "No se encontró el cierre operativo"));
    }

}
