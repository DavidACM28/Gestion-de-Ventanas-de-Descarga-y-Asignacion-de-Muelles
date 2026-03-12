package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.MuelleDTO;
import pe.incubadora.backend.entities.MuelleEntity;
import pe.incubadora.backend.repositories.MuelleRepository;
import pe.incubadora.backend.utils.UpdateMuelleResult;

@Service
public class MuelleService {
    @Autowired
    private MuelleRepository muelleRepository;

    @Transactional
    public boolean crearMuelle(MuelleDTO muelle) {
        if (!muelle.getTipoCargaPermitida().equalsIgnoreCase("seca") &&
            !muelle.getTipoCargaPermitida().equalsIgnoreCase("refrigerada")) {
            return false;
        }
        MuelleEntity muelleEntity = new MuelleEntity();
        muelleEntity.setActivo(true);
        muelleEntity.setTipoCargaPermitida(muelle.getTipoCargaPermitida().toUpperCase());
        muelleEntity.setCapacidadToneladas(muelle.getCapacidadToneladas());
        muelleEntity.setCodigo(muelle.getCodigo());
        muelleEntity.setNombre(muelle.getNombre());
        muelleRepository.save(muelleEntity);
        return true;
    }

    @Transactional
    public UpdateMuelleResult actualizarMuelle(MuelleDTO muelle, Long id) {
        MuelleEntity muelleEntity = muelleRepository.findById(id).orElse(null);
        if (muelleEntity == null) {
            return UpdateMuelleResult.MUELLE_NOT_FOUND;
        }
        if (muelle.getCodigo() != null) {
            muelleEntity.setCodigo(muelle.getCodigo());
        }
        if (muelle.getNombre() != null) {
            if (muelle.getNombre().trim().length() < 3) {
                return UpdateMuelleResult.NOMBRE_INVALIDO;
            }
            muelleEntity.setNombre(muelle.getNombre());
        }
        if (muelle.getTipoCargaPermitida() != null) {
            if (!muelle.getTipoCargaPermitida().equalsIgnoreCase("seca") &&
                !muelle.getTipoCargaPermitida().equalsIgnoreCase("refrigerada"))
            {
                return UpdateMuelleResult.TIPO_CARGA_INVALIDA;
            }
            muelleEntity.setTipoCargaPermitida(muelle.getTipoCargaPermitida().toUpperCase());
        }
        if (muelle.getCapacidadToneladas() != null) {
            if (muelle.getCapacidadToneladas().doubleValue() < 0.1) {
                return UpdateMuelleResult.CAPACIDAD_INVALIDA;
            }
            muelleEntity.setCapacidadToneladas(muelle.getCapacidadToneladas());
        }
        muelleRepository.save(muelleEntity);
        return UpdateMuelleResult.UPDATED;
    }

    public Page<MuelleEntity> getMuelles(Pageable pageable) {
        return muelleRepository.findAll(pageable);
    }

    public MuelleEntity getMuelle(Long id) {
        return muelleRepository.findById(id).orElse(null);
    }
}
