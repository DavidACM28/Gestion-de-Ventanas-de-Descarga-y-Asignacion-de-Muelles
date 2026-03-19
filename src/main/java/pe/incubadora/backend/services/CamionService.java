package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.CamionDTO;
import pe.incubadora.backend.dtos.UpdateCamionDTO;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.CamionRepository;
import pe.incubadora.backend.repositories.ColaEsperaRepository;
import pe.incubadora.backend.repositories.EmpresaTransportistaRepository;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.utils.*;

import java.util.List;

/**
 * Encapsulates business logic related to trucks.
 *
 * <p>The service handles creation, retrieval and updates while enforcing
 * company assignment and active waiting queue restrictions.</p>
 */
@Service
public class CamionService {
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private EmpresaTransportistaRepository empresaTransportistaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ColaEsperaRepository colaEsperaRepository;

    /**
     * Creates a new active truck.
     *
     * @param camion truck payload
     * @return creation result
     */
    @Transactional
    public CreateCamionResult crearCamion(CamionDTO camion) {
        EmpresaTransportistaEntity empresaTransportistaEntity = empresaTransportistaRepository.findById(camion.getEmpresaId()).orElse(null);
        if (empresaTransportistaEntity == null) {
            return CreateCamionResult.EMPRESA_NOT_FOUND;
        }
        if (!camion.getTipoCarga().equalsIgnoreCase("seca") && !camion.getTipoCarga().equalsIgnoreCase("refrigerada")) {
            return CreateCamionResult.TIPO_CARGA_NOT_VALID;
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

    /**
     * Returns trucks visible to the authenticated user.
     *
     * @param page requested pagination information
     * @return paginated truck data
     */
    public Page<CamionEntity> getCamiones(Pageable page) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roles.contains("ROLE_TRANSPORTISTA")) {
            UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
            if (usuario == null || usuario.getCamion() == null) {
                return Page.empty(page);
            }
            return new PageImpl<>(List.of(usuario.getCamion()), page, 1);
        }
        return camionRepository.findAll(page);
    }

    /**
     * Updates an existing truck after validating all mutable attributes.
     *
     * @param camion partial update payload
     * @param id     truck identifier
     * @return update result
     */
    @Transactional
    public UpdateCamionResult updateCamion(UpdateCamionDTO camion, Long id) {
        CamionEntity camionEntity = camionRepository.findById(id).orElse(null);
        if (camionEntity == null) {
            return UpdateCamionResult.CAMION_NOT_FOUND;
        }
        UpdateCamionResult resultado = validarUpdateCamion(camion, camionEntity);
        if (resultado != null) {
            return resultado;
        }
        aplicarCambios(camion, camionEntity);
        camionRepository.save(camionEntity);
        return UpdateCamionResult.UPDATED;
    }

    /**
     * Retrieves a single truck visible to the authenticated user.
     *
     * @param id truck identifier
     * @return truck entity or {@code null} when it cannot be accessed
     */
    public CamionEntity getCamion(Long id) {
        CamionEntity camionEntity = camionRepository.findById(id).orElse(null);
        if (camionEntity == null) {
            return null;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roles.contains("ROLE_TRANSPORTISTA")) {
            UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
            if (usuario == null || usuario.getCamion() == null) {
                return null;
            }
            if (!usuario.getCamion().getId().equals(camionEntity.getId())) {
                return null;
            }
        }
        return camionEntity;
    }

    /**
     * Removes the current truck assignment from the associated transport user.
     *
     * @param id truck identifier
     * @return unassignment result
     */
    @Transactional
    public DesasignarCamionResult desasignarCamion(Long id) {
        CamionEntity camion = camionRepository.findById(id).orElse(null);
        if (camion == null) {
            return DesasignarCamionResult.CAMION_NOT_FOUND;
        }
        UsuarioEntity usuario = usuarioRepository.getByCamionId(id);
        if (usuario == null) {
            return DesasignarCamionResult.USUARIO_NOT_FOUND;
        }
        usuario.setCamion(null);
        usuarioRepository.save(usuario);
        return DesasignarCamionResult.DESASIGNADO;
    }

    /**
     * Assigns a truck to a transport user after validating both sides of the relation.
     *
     * @param idCamion truck identifier
     * @param idUsuario user identifier
     * @return assignment result
     */
    @Transactional
    public AsignarCamionResult asignarCamion(Long idCamion, Long idUsuario) {
        CamionEntity camion = camionRepository.findById(idCamion).orElse(null);
        if (camion == null) {
            return AsignarCamionResult.CAMION_NOT_FOUND;
        }
        if (usuarioRepository.existsByCamionId(idCamion)) {
            return AsignarCamionResult.CAMION_ASIGNADO;
        }
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) {
            return AsignarCamionResult.USUARIO_NOT_FOUND;
        }
        if (!usuario.getRol().equals(Rol.TRANSPORTISTA)) {
            return AsignarCamionResult.USUARIO_ADMINISTRATIVO;
        }
        if (usuario.getCamion() != null) {
            return AsignarCamionResult.USUARIO_CON_CAMION;
        }
        usuario.setCamion(camion);
        usuarioRepository.save(usuario);
        return AsignarCamionResult.ASIGNADO;
    }

    /**
     * Validates a truck update request before applying any modification.
     *
     * @param camion       incoming update payload
     * @param camionEntity persisted entity to validate against
     * @return a validation result or {@code null} when the request is valid
     */
    private UpdateCamionResult validarUpdateCamion(UpdateCamionDTO camion, CamionEntity camionEntity) {
        if (camion.getEmpresaId() != null) {
            if (!empresaTransportistaRepository.existsById(camion.getEmpresaId())) {
                return UpdateCamionResult.EMPRESA_NOT_FOUND;
            }
            if (usuarioRepository.existsByCamionId(camionEntity.getId())) {
                return UpdateCamionResult.CAMION_ASIGNADO;
            }
        }
        if (camion.getTipoCarga() != null) {
            if (!camion.getTipoCarga().equalsIgnoreCase("seca") && !camion.getTipoCarga().equalsIgnoreCase("refrigerada")) {
                return UpdateCamionResult.TIPO_CARGA_INVALIDA;
            }
        }
        if (camion.getCapacidadToneladas() != null && camion.getCapacidadToneladas().doubleValue() < 0.1) {
            return UpdateCamionResult.CAPACIDAD_INVALIDA;
        }
        if (camion.getActivo() != null) {
            if (!camion.getActivo() && colaEsperaRepository.existsByCamionIdAndEstado(camionEntity.getId(), "ACTIVA")) {
                return UpdateCamionResult.COLA_ESPERA_ACTIVA;
            }
        }
        return null;
    }

    /**
     * Applies the validated truck changes to the managed entity.
     *
     * @param camion       validated update payload
     * @param camionEntity entity to mutate
     */
    private void aplicarCambios(UpdateCamionDTO camion, CamionEntity camionEntity) {
        if (camion.getEmpresaId() != null) {
            camionEntity.setEmpresa(empresaTransportistaRepository.findById(camion.getEmpresaId()).orElse(null));
        }
        if (camion.getTipoCarga() != null) {
            camionEntity.setTipoCarga(camion.getTipoCarga().toUpperCase());
        }
        if (camion.getCapacidadToneladas() != null) {
            camionEntity.setCapacidadToneladas(camion.getCapacidadToneladas());
        }
        if (camion.getActivo() != null) {
            camionEntity.setActivo(camion.getActivo());
        }
    }
}
