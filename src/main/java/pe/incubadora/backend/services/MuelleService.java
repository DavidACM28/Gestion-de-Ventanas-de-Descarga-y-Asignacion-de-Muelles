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

/**
 * Encapsulates dock business logic.
 */
@Service
public class MuelleService {
    @Autowired
    private MuelleRepository muelleRepository;

    /**
     * Creates a dock after validating its supported cargo type.
     *
     * @param muelle dock payload
     * @return {@code true} when the dock is valid and was persisted
     */
    @Transactional
    public boolean crearMuelle(MuelleDTO muelle) {
        if (!muelle.getTipoCargaPermitida().equalsIgnoreCase("seca") &&
            !muelle.getTipoCargaPermitida().equalsIgnoreCase("refrigerada") &&
            !muelle.getTipoCargaPermitida().equalsIgnoreCase("mixta")) {
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

    /**
     * Updates an existing dock.
     *
     * @param muelle partial update payload
     * @param id dock identifier
     * @return update result
     */
    @Transactional
    public UpdateMuelleResult actualizarMuelle(MuelleDTO muelle, Long id) {
        MuelleEntity muelleEntity = muelleRepository.findById(id).orElse(null);
        if (muelleEntity == null) {
            return UpdateMuelleResult.MUELLE_NOT_FOUND;
        }
        UpdateMuelleResult resultado = validarUpdateMuelle(muelle);
        if (resultado != null) {
            return resultado;
        }
        aplicarCambios(muelle, muelleEntity);
        muelleRepository.save(muelleEntity);
        return UpdateMuelleResult.UPDATED;
    }

    /**
     * Returns paginated dock data.
     *
     * @param pageable pagination information
     * @return paginated docks
     */
    public Page<MuelleEntity> getMuelles(Pageable pageable) {
        return muelleRepository.findAll(pageable);
    }

    /**
     * Retrieves a dock by identifier.
     *
     * @param id dock identifier
     * @return dock entity or {@code null} when not found
     */
    public MuelleEntity getMuelle(Long id) {
        return muelleRepository.findById(id).orElse(null);
    }

    /**
     * Validates mutable dock fields before applying an update.
     *
     * @param muelle incoming update payload
     * @return validation result or {@code null} when valid
     */
    private UpdateMuelleResult validarUpdateMuelle(MuelleDTO muelle) {
        if (muelle.getNombre() != null && muelle.getNombre().trim().length() < 3) {
            return UpdateMuelleResult.NOMBRE_INVALIDO;
        }
        if (muelle.getTipoCargaPermitida() != null) {
            if (!muelle.getTipoCargaPermitida().equalsIgnoreCase("seca") &&
                !muelle.getTipoCargaPermitida().equalsIgnoreCase("refrigerada") &&
                !muelle.getTipoCargaPermitida().equalsIgnoreCase("mixta")) {
                return UpdateMuelleResult.TIPO_CARGA_INVALIDA;
            }
        }
        if (muelle.getCapacidadToneladas() != null && muelle.getCapacidadToneladas().doubleValue() < 0.1) {
            return UpdateMuelleResult.CAPACIDAD_INVALIDA;
        }
        return null;
    }

    /**
     * Applies already validated dock changes to the managed entity.
     *
     * @param muelle validated update payload
     * @param muelleEntity entity to mutate
     */
    private void aplicarCambios(MuelleDTO muelle, MuelleEntity muelleEntity) {
        if (muelle.getCodigo() != null) {
            muelleEntity.setCodigo(muelle.getCodigo());
        }
        if (muelle.getNombre() != null) {
            muelleEntity.setNombre(muelle.getNombre());
        }
        if (muelle.getTipoCargaPermitida() != null) {
            muelleEntity.setTipoCargaPermitida(muelle.getTipoCargaPermitida().toUpperCase());
        }
        if (muelle.getCapacidadToneladas() != null) {
            muelleEntity.setCapacidadToneladas(muelle.getCapacidadToneladas());
        }
    }
}
