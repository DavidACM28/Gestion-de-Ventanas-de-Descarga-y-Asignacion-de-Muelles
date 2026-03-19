package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.incubadora.backend.dtos.ColaEsperaDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.ReservaDTO;
import pe.incubadora.backend.entities.ReservaDescargaEntity;
import pe.incubadora.backend.services.ColaEsperaService;
import pe.incubadora.backend.services.ReservaDescargaService;
import pe.incubadora.backend.utils.CambiarEstadoReservaResult;
import pe.incubadora.backend.utils.CreateColaEsperaResult;
import pe.incubadora.backend.utils.CreateReservaDescargaResult;
import pe.incubadora.backend.utils.UpdateReservaResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for reservation lifecycle operations.
 *
 * <p>This controller concentrates the main reservation workflow:
 * creation, lookup, update, filtering and state transitions.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ReservaDescargaController {
    @Autowired
    private ReservaDescargaService reservaDescargaService;
    @Autowired
    private ColaEsperaService colaEsperaService;

    /**
     * Handles invalid request parameter types used in reservation filters.
     *
     * @return a standardized validation error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatchException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    /**
     * Handles invalid request arguments in filter parsing.
     *
     * @return a standardized validation error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponseDTO("VALIDATION_ERROR", "Asegúrese de que los filtros se envíen con el formato correcto"));
    }

    /**
     * Handles missing pagination parameters for filter endpoints.
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
     * Creates a reservation request.
     *
     * <p>If the reservation collides with an occupied slot and the payload
     * explicitly requests waiting queue fallback, the controller delegates
     * queue creation to the waiting queue service.</p>
     *
     * @param reservaDTO reservation payload
     * @param result validation result populated by Spring
     * @return creation, fallback or validation response
     */
    @PostMapping("/reservas")
    private ResponseEntity<Object> crearReserva(@Valid @RequestBody ReservaDTO reservaDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            CreateReservaDescargaResult resultado = reservaDescargaService.crearReserva(reservaDTO);
            return switch (resultado) {
                case MUELLE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MUELLE_NOT_FOUND", "Muelle no encontrado"));
                case CAMION_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("CAMION_NOT_FOUND", "Camión no encontrado"));
                case TIPO_CARGA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El tipo de carga del camión y del muelle no coinciden"));
                case PESO_EXCEDE_MUELLE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El peso de la carga excede el peso máximo para este muelle"));
                case FECHA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida, use formato: yyyy-MM-dd"));
                case HORA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Hora inválida, use formato: HH:mm"));
                case DURACION_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Duración inválida, La duración en minutos debe ser: 30 o 60 o 90 o 120"));
                case FECHA_PASADA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha no puede ser anterior a hoy"));
                case CIERRE_CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("CIERRE_CONFLICT", "Hay un cierre operativo que no permite agendar la reserva"));
                case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(
                    "Reserva para el " + reservaDTO.getFecha() + " a las " + reservaDTO.getHoraInicio() + "Creada con éxito");
            };
        } catch (DataIntegrityViolationException e) {
            if (!reservaDTO.getColaEspera()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("RESERVA_CONFLICT", "Ya existe una reserva en este rango de hora"));
            }
            ColaEsperaDTO colaEsperaDTO = new ColaEsperaDTO();
            colaEsperaDTO.setCamionId(reservaDTO.getCamionId());
            colaEsperaDTO.setFecha(reservaDTO.getFecha());
            CreateColaEsperaResult resultado = colaEsperaService.createDesdeReserva(colaEsperaDTO);
            return switch (resultado) {
                case CAMION_EN_LISTA -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("RESERVA_CONFLICT", "Ya existe una reserva en este rango de hora y el camión ya está en la lista de espera"));
                case COLA_LLENA -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("RESERVA_CONFLICT", "Ya existe una reserva en este rango de hora y la cola está llena"));
                case CREATED -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("RESERVA_CONFLICT", "Ya existe una reserva en este rango de hora, se agendó en lista de espera"));
                default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error desconocido");
            };
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "Ocurrió un error inesperado al crear la reserva"));
        }
    }

    /**
     * Updates a reservation and recalculates its occupied slots when needed.
     *
     * @param reservaDTO partial update payload
     * @param id reservation identifier
     * @return success or validation/domain error response
     */
    @PutMapping("/reservas/{id}")
    private ResponseEntity<Object> updateReserva(@RequestBody ReservaDTO reservaDTO, @PathVariable Long id) {
        try {
            UpdateReservaResult resultado = reservaDescargaService.updateReserva(reservaDTO, id);
            return switch (resultado) {
                case RESERVA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("RESERVA_NOT_FOUND", "Reserva no encontrada"));
                case MUELLE_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("MUELLE_NOT_FOUND", "Muelle no encontrado"));
                case CAMION_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("CAMION_NOT_FOUND", "Camión no encontrado"));
                case NO_COINCIDEN -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El tipo de carga del camión y del muelle no coinciden"));
                case FECHA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Fecha inválida, use formato: yyyy-MM-dd"));
                case HORA_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Hora inválida, use formato: HH:mm"));
                case FECHA_PASADA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La fecha no puede ser anterior a hoy"));
                case HORA_MUY_CERCANA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La nueva hora debe tener por lo menos 30 minutos de diferencia con la hora actual"));
                case DURACION_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "Duración inválida, La duración en minutos debe ser: 30 o 60 o 90 o 120"));
                case PESO_EXCEDE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El peso de la carga excede el peso máximo para este muelle"));
                case TIPO_MERCADERIA_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El tipo de mercadería debe tener al menos 3 caracteres"));
                case CIERRE_CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponseDTO("CIERRE_CONFLICT", "Hay un cierre operativo que no permite agendar la reserva"));
                case UPDATED -> ResponseEntity.status(HttpStatus.OK).body("La reserva se actualizó con éxito");
            };
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("RESERVA_CONFLICT", "Ya existe una reserva en este rango de hora"));
        }
    }

    /**
     * Retrieves a reservation by identifier.
     *
     * @param id reservation identifier
     * @return the reservation or a not found response
     */
    @GetMapping("/reservas/{id}")
    public ResponseEntity<Object> getReserva(@PathVariable Long id) {
        ReservaDescargaEntity reservaDescargaEntity = reservaDescargaService.getReserva(id);
        if (reservaDescargaEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("RESERVA_NOT_FOUND", "No se encontró la reserva"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(reservaDescargaEntity);
    }

    /**
     * Retrieves reservations using optional filters and pagination.
     *
     * @param muelleId dock filter
     * @param camionId truck filter
     * @param empresaId company filter
     * @param fechaDesde lower date bound
     * @param fechaHasta upper date bound
     * @param estado status filter
     * @param tipoCarga cargo type filter
     * @param page zero-based page number
     * @param size page size
     * @param sort sort direction keyword
     * @return paginated reservation data
     */
    @GetMapping("/reservas")
    public ResponseEntity<Object> getReservas(
        @RequestParam(required = false) Long muelleId, @RequestParam(required = false) Long camionId,
        @RequestParam(required = false) Long empresaId, @RequestParam(required = false) String fechaDesde,
        @RequestParam(required = false) String fechaHasta, @RequestParam(required = false) String estado,
        @RequestParam(required = false) String tipoCarga, @RequestParam int page,
        @RequestParam int size, String sort) {

        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde, DateTimeFormatter.ISO_DATE) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta, DateTimeFormatter.ISO_DATE) : null;
        if (desde != null && hasta != null && !desde.isBefore(hasta) && !desde.isEqual(hasta)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "La fecha límite de búsqueda no puede ser anterior a la fecha de inicio de búsqueda"));
        }
        Page<ReservaDescargaEntity> reservas =
            reservaDescargaService.getReservasConFiltros(muelleId, camionId, empresaId, desde, hasta, estado, tipoCarga, page, size, sort);
        return ResponseEntity.status(HttpStatus.OK).body(reservas);
    }

    /**
     * Moves a reservation from {@code SOLICITADA} to {@code CONFIRMADA}.
     *
     * @param id reservation identifier
     * @return state transition result
     */
    @PatchMapping("/reservas/{id}/confirmar")
    public ResponseEntity<Object> confirmarReserva(@PathVariable Long id) {
        CambiarEstadoReservaResult resultado = reservaDescargaService.confirmarReserva(id);
        return switch (resultado) {
            case RESERVA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("RESERVA_NOT_FOUND", "No se encontró la reserva"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Estado inválido, " +
                    "solo se pueden confirmar reservaciones con estado solicitada"));
            case OK -> ResponseEntity.status(HttpStatus.OK).body("Se confirmó la reserva");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error desconocido");
        };
    }

    /**
     * Performs check-in for a confirmed reservation within the allowed time window.
     *
     * @param id reservation identifier
     * @return state transition result
     */
    @PatchMapping("/reservas/{id}/check-in")
    public ResponseEntity<Object> checkInReserva(@PathVariable Long id) {
        CambiarEstadoReservaResult resultado = reservaDescargaService.checkInReserva(id);
        return switch (resultado) {
            case RESERVA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("RESERVA_NOT_FOUND", "No se encontró la reserva"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Estado inválido, " +
                    "solo se puede hacer check in a reservaciones con estado confirmada"));
            case FUERA_DE_VENTANA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Solo se puede hacer chek in desde 30 minutos " +
                    "antes de la hora de reserva y hasta 20 minutos después de la hora de reserva"));
            case OK -> ResponseEntity.status(HttpStatus.OK).body("Se hizó chek in a la reserva");
        };
    }

    /**
     * Marks a checked-in reservation as currently unloading.
     *
     * @param id reservation identifier
     * @return state transition result
     */
    @PatchMapping("/reservas/{id}/iniciar-descarga")
    public ResponseEntity<Object> iniciarDescargaReserva(@PathVariable Long id) {
        CambiarEstadoReservaResult resultado = reservaDescargaService.iniciarReserva(id);
        return switch (resultado) {
            case RESERVA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("RESERVA_NOT_FOUND", "No se encontró la reserva"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Estado inválido, " +
                    "solo se pueden empezar a descargar reservaciones con estado chek in"));
            case OK -> ResponseEntity.status(HttpStatus.OK).body("Se inició la descarga");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error desconocido");
        };
    }

    /**
     * Finalizes an unloading reservation.
     *
     * @param id reservation identifier
     * @return state transition result
     */
    @PatchMapping("/reservas/{id}/finalizar")
    public ResponseEntity<Object> finalizarReserva(@PathVariable Long id) {
        CambiarEstadoReservaResult resultado = reservaDescargaService.finalizarReserva(id);
        return switch (resultado) {
            case RESERVA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("RESERVA_NOT_FOUND", "No se encontró la reserva"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Estado inválido, " +
                    "solo se pueden finalizar reservaciones con estado en descarga"));
            case OK -> ResponseEntity.status(HttpStatus.OK).body("Se finalizó la reserva");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error desconocido");
        };
    }

    /**
     * Cancels a reservation according to ownership and cancellation window rules.
     *
     * @param id reservation identifier
     * @return state transition result
     */
    @PatchMapping("/reservas/{id}/cancelar")
    public ResponseEntity<Object> cancelarReserva(@PathVariable Long id) {
        CambiarEstadoReservaResult resultado = reservaDescargaService.cancelarReserva(id);
        return switch (resultado) {
            case RESERVA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("RESERVA_NOT_FOUND", "No se encontró la reserva"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Estado inválido, " +
                    "solo se puede cancelar reservaciones con estado solicitada, confirmada o check in"));
            case FUERA_DE_VENTANA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Solo se puede cancelar una reserva con 3 " +
                    "horas de anticipación"));
            case OK -> ResponseEntity.status(HttpStatus.OK).body("Se canceló a la reserva");
        };
    }

    /**
     * Marks a confirmed reservation as no-show.
     *
     * @param id reservation identifier
     * @return state transition result
     */
    @PatchMapping("/reservas/{id}/no-show")
    public ResponseEntity<Object> noShowReserva(@PathVariable Long id) {
        CambiarEstadoReservaResult resultado = reservaDescargaService.noShowReserva(id);
        return switch (resultado) {
            case RESERVA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("RESERVA_NOT_FOUND", "No se encontró la reserva"));
            case ESTADO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Estado inválido, " +
                    "solo se puede marcar como no show reservaciones con estado confirmada"));
            case OK -> ResponseEntity.status(HttpStatus.OK).body("Se marcó como no show");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error desconocido");
        };
    }
}
