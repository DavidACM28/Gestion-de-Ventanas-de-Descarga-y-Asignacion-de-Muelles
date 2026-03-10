package pe.incubadora.backend.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.incubadora.backend.dtos.EmpresaTransportistaDTO;
import pe.incubadora.backend.dtos.ErrorResponseDTO;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
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

    @GetMapping("/empresas")
    private ResponseEntity<Object> getEmpresas(@RequestParam int page) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        return ResponseEntity.ok().body(empresaTransportistaService.getEmpresas(pageable));
    }

    @GetMapping("/empresas/{id}")
    private ResponseEntity<Object> getEmpresasById(@PathVariable Long id) {
        EmpresaTransportistaEntity empresa = empresaTransportistaService.getEmpresa(id);
        if (empresa == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("EMPRESA_NOT_FOUND", "Empresa no encontrada"));
        }
        return  ResponseEntity.status(HttpStatus.OK).body(empresa);
    }

}
