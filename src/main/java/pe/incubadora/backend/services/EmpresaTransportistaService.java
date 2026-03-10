package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
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

    public Page<EmpresaTransportistaEntity> getEmpresas(Pageable page) {
        return empresaTransportistaRepository.findAll(page);
    }

    public EmpresaTransportistaEntity getEmpresa(Long id) {
        return empresaTransportistaRepository.findById(id).orElse(null);
    }
}
