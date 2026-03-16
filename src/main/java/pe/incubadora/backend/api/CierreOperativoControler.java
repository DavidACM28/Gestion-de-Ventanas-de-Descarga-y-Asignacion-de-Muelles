package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.CierreOperativoDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.services.CierreOperativoService;
import pe.incubadora.backend.utils.CreateCierreOperativoResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CierreOperativoControler {
    @Autowired
    private CierreOperativoService cierreOperativoService;

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

    @DeleteMapping("/cierres/{id}")
    public ResponseEntity<Object> eliminarCierreOperativo(@PathVariable Long id) {
        if (cierreOperativoService.deleteCierreOperativo(id)) {
            return ResponseEntity.ok().body("Se elimino el cierre operativo");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErrorResponseDTO("CIERRE_NOT_FOUND", "No se encontró el cierre operativo"));
    }

}
