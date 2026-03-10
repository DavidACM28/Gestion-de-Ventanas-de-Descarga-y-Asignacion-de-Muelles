package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.UsuarioRepository;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    public UsuarioEntity findByUsername(String username) {
        return usuarioRepository.findByUsername(username).orElse(null);
    }
}
