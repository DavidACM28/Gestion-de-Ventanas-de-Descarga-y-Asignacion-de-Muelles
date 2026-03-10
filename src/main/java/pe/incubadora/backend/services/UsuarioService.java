package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.auth.RegisterDTO;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.CamionRepository;
import pe.incubadora.backend.repositories.EmpresaTransportistaRepository;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.utils.RegisterUsuarioResult;
import pe.incubadora.backend.utils.Rol;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private EmpresaTransportistaRepository empresaTransportistaRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioEntity findByUsername(String username) {
        return usuarioRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public RegisterUsuarioResult register(RegisterDTO dto) {
        Rol rol;

        try {
            rol = Rol.valueOf(dto.getRol().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RegisterUsuarioResult.ROL_NOT_FOUND;
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(encoder.encode(dto.getPassword()));
        usuario.setRol(rol);
        usuario.setActivo(true);

        switch (rol) {
            case TRANSPORTISTA:
                if (dto.getEmpresaId() == null || dto.getCamionId() == null) {
                    return RegisterUsuarioResult.ROL_TRANSPORTISTA_CONFLICT;
                }

                EmpresaTransportistaEntity empresa = empresaTransportistaRepository.findById(dto.getEmpresaId()).orElse(null);
                CamionEntity camion = camionRepository.findById(dto.getCamionId()).orElse(null);

                if (empresa == null) {
                    return RegisterUsuarioResult.EMPRESA_NOT_FOUND;
                }
                if (camion == null) {
                    return RegisterUsuarioResult.CAMION_NOT_FOUND;
                }
                if (usuarioRepository.existsByCamionId(dto.getCamionId())) {
                    return RegisterUsuarioResult.CAMION_CONFLICT;
                }
                if (!camionRepository.existsByIdAndEmpresaId(dto.getCamionId(), dto.getEmpresaId())) {
                    return RegisterUsuarioResult.CAMION_NOT_MATCH;
                }
                usuario.setEmpresa(empresa);
                usuario.setCamion(camion);
                break;


            case ADMIN, OPERADOR:
                if (dto.getEmpresaId() != null || dto.getCamionId() != null) {
                    return RegisterUsuarioResult.ROL_ADMINISTRATIVO_CONFLICT;
                }
                break;
        }

        usuarioRepository.save(usuario);
        return RegisterUsuarioResult.CREATED;
    }
}
