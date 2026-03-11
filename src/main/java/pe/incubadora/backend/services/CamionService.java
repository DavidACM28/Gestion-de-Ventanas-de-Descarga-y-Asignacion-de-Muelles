package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.dtos.CamionDTO;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
import pe.incubadora.backend.repositories.CamionRepository;
import pe.incubadora.backend.repositories.EmpresaTransportistaRepository;
import pe.incubadora.backend.utils.CreateCamionResult;

@Service
public class CamionService {
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private EmpresaTransportistaRepository empresaTransportistaRepository;

    public CreateCamionResult crearCamion(CamionDTO camion) {
        EmpresaTransportistaEntity empresaTransportistaEntity = empresaTransportistaRepository.findById(camion.getEmpresaId()).orElse(null);
        if (empresaTransportistaEntity == null) {
            return CreateCamionResult.EMPRESA_NOT_FOUND;
        }
        if (!camion.getTipoCarga().equalsIgnoreCase("seca") && !camion.getTipoCarga().equalsIgnoreCase("refrigerada")) {
            return  CreateCamionResult.TIPO_CARGA_NOT_VALID;
        }
        CamionEntity camionEntity = new CamionEntity();
        camionEntity.setActivo(true);
        camionEntity.setPlaca(camion.getPlaca());
        camionEntity.setTipoCarga(camion.getTipoCarga().toUpperCase());
        camionEntity.setEmpresa(empresaTransportistaEntity);
        camionEntity.setCapacidadToneladas(camion.getCapacidadToneladas());
        camionRepository.save(camionEntity);
        return CreateCamionResult.CREATED;
    }
}
