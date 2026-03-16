package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.incubadora.backend.dtos.ColaEsperaDTO;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.entities.ColaEsperaEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.CamionRepository;
import pe.incubadora.backend.repositories.ColaEsperaRepository;
import pe.incubadora.backend.repositories.UsuarioRepository;
import pe.incubadora.backend.utils.CreateColaEsperaResult;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ColaEsperaService {
    @Autowired
    private ColaEsperaRepository colaEsperaRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    public CreateColaEsperaResult createColaEspera(ColaEsperaDTO colaEspera) {
        CamionEntity camionEntity = camionRepository.findById(colaEspera.getCamionId()).orElse(null);
        LocalDate fecha;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        assert usuario != null;
        if (camionEntity == null) {
            return CreateColaEsperaResult.CAMION_NOT_FOUND;
        }
        if (roles.contains("ROLE_TRANSPORTISTA") && !usuario.getCamion().equals(camionEntity)) {
            return CreateColaEsperaResult.CAMION_NOT_FOUND;
        }
        try {
            fecha = LocalDate.parse(colaEspera.getFecha());
        } catch (DateTimeParseException e) {
            return CreateColaEsperaResult.FECHA_INVALIDA;
        }
        if (!colaEspera.getTipoCarga().equalsIgnoreCase("seca") && !colaEspera.getTipoCarga().equalsIgnoreCase("refrigerada")) {
            return CreateColaEsperaResult.CARGA_INVALIDA;
        }
        if (colaEsperaRepository.existsByCamionIdAndFechaAndEstado(camionEntity.getId(), fecha, "ACTIVA")) {
            return CreateColaEsperaResult.CAMION_EN_LISTA;
        }
        if (!camionEntity.getTipoCarga().equalsIgnoreCase(colaEspera.getTipoCarga())) {
            return CreateColaEsperaResult.NO_COINCIDEN;
        }
        List<ColaEsperaEntity> cola = colaEsperaRepository.findByFechaAndTipoCargaAndEstado(fecha, colaEspera.getTipoCarga().toUpperCase(), "ACTIVA");
        if (cola.size() >= 5) {
            return CreateColaEsperaResult.COLA_LLENA;
        }
        ColaEsperaEntity colaEsperaEntity = new ColaEsperaEntity();
        colaEsperaEntity.setCamion(camionEntity);
        colaEsperaEntity.setFecha(fecha);
        colaEsperaEntity.setTipoCarga(colaEspera.getTipoCarga().toUpperCase());
        colaEsperaEntity.setPrioridad(cola.size() + 1);
        colaEsperaEntity.setEstado("ACTIVA");
        colaEsperaEntity.setObservacion(colaEspera.getObservacion());
        colaEsperaRepository.save(colaEsperaEntity);
        return CreateColaEsperaResult.CREATED;
    }
}
