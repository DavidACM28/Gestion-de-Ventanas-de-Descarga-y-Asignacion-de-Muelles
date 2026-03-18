package pe.incubadora.backend.services;

import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import pe.incubadora.backend.dtos.EmpresaTransportistaDTO;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
import pe.incubadora.backend.repositories.EmpresaTransportistaRepository;
import pe.incubadora.backend.utils.UpdateEmpresaResult;

@Service
public class EmpresaTransportistaService {
    @Autowired
    private EmpresaTransportistaRepository empresaTransportistaRepository;

    @Transactional
    public boolean crearEmpresa(EmpresaTransportistaDTO empresa){
        if (empresa.getRuc().trim().length() != 11){
            return false;
        }

        EmpresaTransportistaEntity empresaTransportistaEntity = new EmpresaTransportistaEntity();
        empresaTransportistaEntity.setActivo(true);
        empresaTransportistaEntity.setRuc(empresa.getRuc());
        empresaTransportistaEntity.setEmail(empresa.getEmail());
        empresaTransportistaEntity.setContactoNombre(empresa.getContactoNombre());
        empresaTransportistaEntity.setContactoTelefono(empresa.getContactoTelefono());
        empresaTransportistaEntity.setRazonSocial(empresa.getRazonSocial());
        empresaTransportistaRepository.save(empresaTransportistaEntity);
        return true;
    }

    @Transactional
    public UpdateEmpresaResult updateEmpresa(EmpresaTransportistaDTO empresa, Long id) {
        EmpresaTransportistaEntity empresaTransportistaEntity = empresaTransportistaRepository.findById(id).orElse(null);
        if (empresaTransportistaEntity == null) {
            return UpdateEmpresaResult.EMPRESA_NOT_FOUND;
        }
        UpdateEmpresaResult resultado = validarUpdateEmpresa(empresa, empresaTransportistaEntity);
        if (resultado != null) {
            return resultado;
        }
        aplicarCambios(empresa, empresaTransportistaEntity);
        empresaTransportistaRepository.save(empresaTransportistaEntity);
        return UpdateEmpresaResult.UPDATED;
    }

    public Page<EmpresaTransportistaEntity> getEmpresas(Pageable page) {
        return empresaTransportistaRepository.findAll(page);
    }

    public EmpresaTransportistaEntity getEmpresa(Long id) {
        return empresaTransportistaRepository.findById(id).orElse(null);
    }

    private UpdateEmpresaResult validarUpdateEmpresa(
        EmpresaTransportistaDTO empresa, EmpresaTransportistaEntity empresaTransportistaEntity) {

        if (empresa.getRuc() != null && !empresa.getRuc().equals(empresaTransportistaEntity.getRuc())) {
            if (!empresa.getRuc().matches("\\d+") || empresa.getRuc().trim().length() != 11) {
                return UpdateEmpresaResult.RUC_INVALIDO;
            }
        }
        if (empresa.getRazonSocial() != null && empresa.getRazonSocial().trim().length() < 3) {
            return UpdateEmpresaResult.RAZON_SOCIAL_INVALIDA;
        }
        if (empresa.getContactoNombre() != null && empresa.getContactoNombre().trim().isEmpty()) {
            return UpdateEmpresaResult.NOMBRE_CONTACTO_INVALIDO;
        }
        if (empresa.getContactoTelefono() != null) {
            if (!empresa.getContactoTelefono().matches("\\d+") || empresa.getContactoTelefono().trim().length() != 9) {
                return UpdateEmpresaResult.TELEFONO_CONTACTO_INVALIDO;
            }
        }
        if (empresa.getEmail() != null) {
            EmailValidator emailValidator = new EmailValidator();
            if (!emailValidator.isValid(empresa.getEmail(), null)) {
                return UpdateEmpresaResult.EMAIL_INVALIDO;
            }
        }
        return null;
    }

    private void aplicarCambios(EmpresaTransportistaDTO empresa, EmpresaTransportistaEntity empresaTransportistaEntity) {
        if (empresa.getRuc() != null && !empresa.getRuc().equals(empresaTransportistaEntity.getRuc())) {
            empresaTransportistaEntity.setRuc(empresa.getRuc());
        }
        if (empresa.getRazonSocial() != null) {
            empresaTransportistaEntity.setRazonSocial(empresa.getRazonSocial());
        }
        if (empresa.getContactoNombre() != null) {
            empresaTransportistaEntity.setContactoNombre(empresa.getContactoNombre());
        }
        if (empresa.getContactoTelefono() != null) {
            empresaTransportistaEntity.setContactoTelefono(empresa.getContactoTelefono());
        }
        if (empresa.getEmail() != null) {
            empresaTransportistaEntity.setEmail(empresa.getEmail());
        }
    }
}
