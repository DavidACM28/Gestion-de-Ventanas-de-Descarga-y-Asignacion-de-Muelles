package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.EmpresaTransportistaDTO;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
import pe.incubadora.backend.repositories.EmpresaTransportistaRepository;

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
}
