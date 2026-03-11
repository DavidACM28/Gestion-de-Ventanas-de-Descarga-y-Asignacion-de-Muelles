package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.dtos.CamionDTO;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.CamionRepository;
import pe.incubadora.backend.repositories.EmpresaTransportistaRepository;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.utils.CreateCamionResult;

import java.util.List;

@Service
public class CamionService {
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private EmpresaTransportistaRepository empresaTransportistaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

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
