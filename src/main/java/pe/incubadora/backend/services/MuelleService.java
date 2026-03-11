package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.MuelleDTO;
import pe.incubadora.backend.entities.MuelleEntity;
import pe.incubadora.backend.repositories.MuelleRepository;

@Service
public class MuelleService {
    @Autowired
    private MuelleRepository muelleRepository;

    @Transactional
    public boolean crearMuelle(MuelleDTO muelle) {
        if (!muelle.getTipoCargaPermitida().equalsIgnoreCase("seca") &&
            !muelle.getTipoCargaPermitida().equalsIgnoreCase("refrigerada"))
        {
            return false;
        }
        MuelleEntity muelleEntity = new MuelleEntity();
        muelleEntity.setActivo(true);
        muelleEntity.setTipoCargaPermitida(muelle.getTipoCargaPermitida());
        muelleEntity.setCapacidadToneladas(muelle.getCapacidadToneladas());
        muelleEntity.setCodigo(muelle.getCodigo());
        muelleEntity.setNombre(muelle.getNombre());
        muelleRepository.save(muelleEntity);
        return true;
    }
}
