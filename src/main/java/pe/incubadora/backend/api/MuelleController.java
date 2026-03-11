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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MuelleController {
    @Autowired
    private MuelleService muelleService;

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
                new ErrorResponseDTO("VALIDATION_ERROR", "Tipo de carga inválida, use: SECA | REFRIGERADA"));
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Ya hay un muelle registrado con este código"));
        }
    }

    @GetMapping("/muelles")
    private ResponseEntity<Object> getMuelles(@RequestParam int page) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        return ResponseEntity.status(HttpStatus.OK).body(muelleService.getMuelles(pageable));
    }

    @GetMapping("/muelles/{id}")
    private ResponseEntity<Object> getMuelle(@PathVariable Long id) {
        MuelleEntity muelleEntity = muelleService.getMuelle(id);
        if (muelleEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Muelle no encontrado");
        }
        return ResponseEntity.status(HttpStatus.OK).body(muelleEntity);
    }

}
