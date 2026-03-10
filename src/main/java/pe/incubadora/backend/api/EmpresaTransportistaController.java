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
import pe.incubadora.backend.dtos.EmpresaTransportistaDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.services.EmpresaTransportistaService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class EmpresaTransportistaController {
    @Autowired
    private EmpresaTransportistaService empresaTransportistaService;

    @PostMapping("/empresas")
    private ResponseEntity<Object> crearEmpresa(@Valid @RequestBody EmpresaTransportistaDTO empresaTransportistaDTO, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            result.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("code", "VALIDATION_ERROR");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try{
            if(empresaTransportistaService.crearEmpresa(empresaTransportistaDTO)){
                return ResponseEntity.status(HttpStatus.CREATED).body(empresaTransportistaDTO);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El RUC debe tener 11 dígitos"));
        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Ya existe una empresa con este RUC"));
        }
    }

}
