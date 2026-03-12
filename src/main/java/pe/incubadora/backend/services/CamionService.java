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
import pe.incubadora.backend.repositories.EmpresaTransportistaRepository;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.utils.CreateCamionResult;
import pe.incubadora.backend.utils.UpdateCamionResult;

import java.util.List;

@Service
public class CamionService {
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private EmpresaTransportistaRepository empresaTransportistaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;


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

    public Page<CamionEntity> getCamiones(Pageable page) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roles.contains("ROLE_TRANSPORTISTA")) {
            UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
            assert usuario != null;
            return new PageImpl<>(List.of(usuario.getCamion()), page, 1);
        }
        return camionRepository.findAll(page);
    }

    @Transactional
    public UpdateCamionResult updateCamion(UpdateCamionDTO camion, Long id) {
        CamionEntity camionEntity = camionRepository.findById(id).orElse(null);
        if (camionEntity == null) {
            return UpdateCamionResult.CAMION_NOT_FOUND;
        }
        if (camion.getEmpresaId() != null) {
            if (!empresaTransportistaRepository.existsById(camion.getEmpresaId())) {
                return UpdateCamionResult.EMPRESA_NOT_FOUND;
            }
            if (usuarioRepository.existsByCamionId(camionEntity.getId())) {
                return UpdateCamionResult.CAMION_ASIGNADO;
            }
            camionEntity.setEmpresa(empresaTransportistaRepository.findById(camion.getEmpresaId()).orElse(null));
        }
        if (camion.getTipoCarga() != null) {
            if (!camion.getTipoCarga().equalsIgnoreCase("seca") &&  !camion.getTipoCarga().equalsIgnoreCase("refrigerada")) {
                return UpdateCamionResult.TIPO_CARGA_INVALIDA;
            }
            camionEntity.setTipoCarga(camion.getTipoCarga().toUpperCase());
        }
        if (camion.getCapacidadToneladas() != null) {
            if (camion.getCapacidadToneladas().doubleValue() < 0.1){
                return UpdateCamionResult.CAPACIDAD_INVALIDA;
            }
            camionEntity.setCapacidadToneladas(camion.getCapacidadToneladas());
        }
        camionRepository.save(camionEntity);
        return UpdateCamionResult.UPDATED;
    }

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
            assert usuario != null;
            if (!usuario.getCamion().getId().equals(camionEntity.getId())) {
                return null;
            }
        }
        return camionEntity;
    }
}
