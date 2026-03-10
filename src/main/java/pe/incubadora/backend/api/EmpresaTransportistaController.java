package pe.incubadora.backend.api;

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

@RestController
@RequestMapping("/api/v1")
public class EmpresaTransportistaController {
    @Autowired
    private EmpresaTransportistaService empresaTransportistaService;

    @PostMapping("/empresas")
    private ResponseEntity<Object> crearEmpresa(@RequestBody EmpresaTransportistaDTO empresaTransportistaDTO, BindingResult result) {
        try{
            empresaTransportistaService.crearEmpresa(empresaTransportistaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(empresaTransportistaDTO);
        }
        catch (Exception ex){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Ya existe una empresa con este ruc"));
        }
    }

}
