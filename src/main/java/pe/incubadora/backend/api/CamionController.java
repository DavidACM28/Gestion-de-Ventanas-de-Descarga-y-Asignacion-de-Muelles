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
import pe.incubadora.backend.dtos.CamionDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.services.CamionService;
import pe.incubadora.backend.utils.CreateCamionResult;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CamionController {
    @Autowired
    private CamionService camionService;

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
        try{
            CreateCamionResult resultado  = camionService.crearCamion(camionDTO);
            return switch (resultado){
                case EMPRESA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("EMPRESA_NOT_FOUND", "La empresa no existe"));
                case TIPO_CARGA_NOT_VALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El tipo de carga no es valido, use: SECA | REFRIGERADA"));
                case CREATED ->  ResponseEntity.status(HttpStatus.CREATED).body("Se creó el camión exitosamente");
            };
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Ya existe un camión con esta placa"));
        }
    }

}
