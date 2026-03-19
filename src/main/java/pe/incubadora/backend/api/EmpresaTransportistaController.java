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
import pe.incubadora.backend.utils.UpdateEmpresaResult;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for transport company management.
 */
@RestController
@RequestMapping("/api/v1")
public class EmpresaTransportistaController {
    @Autowired
    private EmpresaTransportistaService empresaTransportistaService;

    /**
     * Creates a new transport company.
     *
     * @param empresaTransportistaDTO company payload
     * @param result validation result populated by Spring
     * @return the created payload or a validation/domain error
     */
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
        try {
            if (empresaTransportistaService.crearEmpresa(empresaTransportistaDTO)) {
                return ResponseEntity.status(HttpStatus.CREATED).body(empresaTransportistaDTO);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El RUC debe tener 11 dígitos"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "Ya existe una empresa con este RUC"));
        }
    }

    /**
     * Updates an existing transport company.
     *
     * @param empresaTransportistaDTO partial update payload
     * @param id company identifier
     * @return the updated entity or a validation/domain error
     */
    @PutMapping("/empresas/{id}")
    private ResponseEntity<Object> updateEmpresa(@RequestBody EmpresaTransportistaDTO empresaTransportistaDTO, @PathVariable Long id) {
        try {
            UpdateEmpresaResult resultado = empresaTransportistaService.updateEmpresa(empresaTransportistaDTO, id);
            return switch (resultado) {
                case EMPRESA_NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ErrorResponseDTO("EMPRESA_NOT_FOUND", "Empresa no encontrada"));
                case RUC_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El RUC ingresado es inválido, debe tener 11 dígitos numéricos"));
                case RAZON_SOCIAL_INVALIDA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "La razón social ingresada es inválida, debe tener al menos 3 caracteres"));
                case NOMBRE_CONTACTO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El nombre de contacto ingresado es invalido"));
                case TELEFONO_CONTACTO_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El número de teléfono ingresado es invalido, deben tener 9 dígitos numéricos"));
                case EMAIL_INVALIDO -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ErrorResponseDTO("VALIDATION_ERROR", "El email ingresado es inválido"));
                case UPDATED ->  ResponseEntity.status(HttpStatus.OK).body(empresaTransportistaService.getEmpresa(id));
            };
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO("VALIDATION_ERROR", "El ruc ingresado ya está registrado"));
        }
    }

    /**
     * Returns a paginated list of transport companies.
     *
     * @param page zero-based page number
     * @return paginated transport company data
     */
    @GetMapping("/empresas")
    private ResponseEntity<Object> getEmpresas(@RequestParam int page) {
        Pageable pageable = Pageable.ofSize(10).withPage(page);
        return ResponseEntity.ok().body(empresaTransportistaService.getEmpresas(pageable));
    }

    /**
     * Retrieves a transport company by identifier.
     *
     * @param id company identifier
     * @return the company or a not found response
     */
    @GetMapping("/empresas/{id}")
    private ResponseEntity<Object> getEmpresasById(@PathVariable Long id) {
        EmpresaTransportistaEntity empresa = empresaTransportistaService.getEmpresa(id);
        if (empresa == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO("EMPRESA_NOT_FOUND", "Empresa no encontrada"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(empresa);
    }

}
