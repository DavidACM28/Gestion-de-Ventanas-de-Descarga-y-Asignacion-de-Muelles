package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.dtos.ReservaDTO;
import pe.incubadora.backend.services.ReservaDescargaService;
import pe.incubadora.backend.utils.CreateReservaDescargaResult;
import pe.incubadora.backend.utils.UpdateReservaResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ReservaDescargaController {
    @Autowired
    private ReservaDescargaService reservaDescargaService;

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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("RESERVA_CONFLICT", "Ya existe una reserva en este rango de hora"));
        }
    }

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
}
