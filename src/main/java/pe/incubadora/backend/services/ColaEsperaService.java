package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.ColaEsperaDTO;
import pe.incubadora.backend.dtos.PromoverColaEsperaDTO;
import pe.incubadora.backend.dtos.ReservaDTO;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.entities.ColaEsperaEntity;
import pe.incubadora.backend.entities.MuelleEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.*;
import pe.incubadora.backend.utils.CancelarColaEsperaResult;
import pe.incubadora.backend.utils.CreateColaEsperaResult;
import pe.incubadora.backend.utils.CreateReservaDescargaResult;
import pe.incubadora.backend.utils.PromoverColaEsperaResult;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ColaEsperaService {
    @Autowired
    private ColaEsperaRepository colaEsperaRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private MuelleRepository muelleRepository;
    @Autowired
    private CierreOperativoRepository cierreOperativoRepository;
    @Autowired
    private ReservaDescargaService reservaDescargaService;


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

    public CreateColaEsperaResult createDesdeReserva(ColaEsperaDTO colaEspera) {
        CamionEntity camion = camionRepository.findById(colaEspera.getCamionId()).orElse(null);
        assert camion != null;
        colaEspera.setTipoCarga(camion.getTipoCarga());
        colaEspera.setObservacion("");
        return createColaEspera(colaEspera);
    }

    public Page<ColaEsperaEntity> getColasConFiltros(
        LocalDate fecha, String tipoCarga, String estado, Integer prioridad, int page, int size, String sort) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        assert usuario != null;

        Specification<ColaEsperaEntity> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (roles.contains("ROLE_TRANSPORTISTA")) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("camion").get("id"), usuario.getCamion().getId()));
        }
        if (fecha != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fecha"), fecha));
        }
        if (tipoCarga != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipoCarga"), tipoCarga));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("estado"), estado));
        }
        if (prioridad != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("prioridad"), prioridad));
        }

        Sort.Direction direction = "descending".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "id"));
        return colaEsperaRepository.findAll(spec, pageable);
    }

    @Transactional
    public CancelarColaEsperaResult cancelarColaEspera(Long id) {
        ColaEsperaEntity colaEsperaEntity = colaEsperaRepository.findById(id).orElse(null);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        assert usuario != null;
        if (colaEsperaEntity == null) {
            return CancelarColaEsperaResult.COLA_NOT_FOUND;
        }
        if (!colaEsperaEntity.getEstado().equalsIgnoreCase("ACTIVA")) {
            return CancelarColaEsperaResult.ESTADO_INVALIDO;
        }
        if (roles.contains("ROLE_TRANSPORTISTA") && !usuario.getCamion().equals(colaEsperaEntity.getCamion())) {
            return CancelarColaEsperaResult.COLA_NOT_FOUND;
        }
        colaEsperaEntity.setEstado("CANCELADA");
        colaEsperaRepository.save(colaEsperaEntity);
        reordenarPrioridades(colaEsperaEntity.getFecha(), colaEsperaEntity.getTipoCarga());
        return CancelarColaEsperaResult.CANCELED;
    }

    @Transactional
    public PromoverColaEsperaResult promoverColaEspera(PromoverColaEsperaDTO dto) {
        MuelleEntity muelle = muelleRepository.findByIdAndActivoTrue(dto.getMuelleId()).orElse(null);
        LocalDate fecha;
        if (muelle == null) {
            return PromoverColaEsperaResult.MUELLE_NOT_FOUND;
        }

        try {
            fecha = LocalDate.parse(dto.getFecha());
        } catch (DateTimeParseException e) {
            return PromoverColaEsperaResult.FECHA_INVALIDA;
        }

        ColaEsperaEntity cola = obtenerSiguienteCola(fecha, muelle.getTipoCargaPermitida());
        if (cola == null) {
            return PromoverColaEsperaResult.SIN_CANDIDATOS;
        }

        ReservaDTO reservaDTO = new ReservaDTO();
        reservaDTO.setMuelleId(muelle.getId());
        reservaDTO.setCamionId(cola.getCamion().getId());
        reservaDTO.setFecha(dto.getFecha());
        reservaDTO.setHoraInicio(dto.getHoraInicio());
        reservaDTO.setDuracionMin(dto.getDuracionMin());
        reservaDTO.setPesoEstimadoToneladas(dto.getPesoEstimadoToneladas());
        reservaDTO.setTipoMercaderia(dto.getTipoMercaderia());
        reservaDTO.setNota(dto.getNota());

        CreateReservaDescargaResult resultado = reservaDescargaService.crearReserva(reservaDTO);
        if (resultado != CreateReservaDescargaResult.CREATED) {
            return mapearResultadoReserva(resultado);
        }

        cola.setEstado("ASIGNADA");
        colaEsperaRepository.save(cola);
        reordenarPrioridades(cola.getFecha(), cola.getTipoCarga());

        return PromoverColaEsperaResult.PROMOTED;
    }

    private ColaEsperaEntity obtenerSiguienteCola(LocalDate fecha, String tipoCargaMuelle) {
        List<ColaEsperaEntity> colasActivas;
        if (tipoCargaMuelle.equalsIgnoreCase("MIXTA")) {
            colasActivas = colaEsperaRepository.findByFechaAndEstadoAndTipoCargaInOrderByPrioridadAscIdAsc(
                fecha, "ACTIVA", List.of("SECA", "REFRIGERADA"));
        } else {
            colasActivas = colaEsperaRepository.findByFechaAndTipoCargaAndEstadoOrderByPrioridadAscIdAsc(
                fecha, tipoCargaMuelle.toUpperCase(), "ACTIVA");
        }
        return colasActivas.stream().findFirst().orElse(null);
    }

    private PromoverColaEsperaResult mapearResultadoReserva(CreateReservaDescargaResult resultado) {
        return switch (resultado) {
            case MUELLE_NOT_FOUND -> PromoverColaEsperaResult.MUELLE_NOT_FOUND;
            case TIPO_CARGA_INVALIDA -> PromoverColaEsperaResult.TIPO_CARGA_INVALIDA;
            case PESO_EXCEDE_MUELLE -> PromoverColaEsperaResult.PESO_EXCEDE_MUELLE;
            case FECHA_INVALIDA -> PromoverColaEsperaResult.FECHA_INVALIDA;
            case HORA_INVALIDA -> PromoverColaEsperaResult.HORA_INVALIDA;
            case DURACION_INVALIDA -> PromoverColaEsperaResult.DURACION_INVALIDA;
            case FECHA_PASADA -> PromoverColaEsperaResult.FECHA_PASADA;
            case CIERRE_CONFLICT -> PromoverColaEsperaResult.CIERRE_CONFLICT;
            case CAMION_NOT_FOUND -> PromoverColaEsperaResult.CAMION_NOT_FOUND;
            case CREATED -> PromoverColaEsperaResult.PROMOTED;
        };
    }

    private void reordenarPrioridades(LocalDate fecha, String tipoCarga) {
        List<ColaEsperaEntity> colasActivas = colaEsperaRepository.findByFechaAndTipoCargaAndEstadoOrderByPrioridadAscIdAsc(
            fecha, tipoCarga, "ACTIVA");
        List<ColaEsperaEntity> colasActualizadas = new ArrayList<>();

        for (int i = 0; i < colasActivas.size(); i++) {
            ColaEsperaEntity colaEsperaEntity = colasActivas.get(i);
            int nuevaPrioridad = i + 1;
            if (colaEsperaEntity.getPrioridad() != nuevaPrioridad) {
                colaEsperaEntity.setPrioridad(nuevaPrioridad);
                colasActualizadas.add(colaEsperaEntity);
            }
        }
        if (!colasActualizadas.isEmpty()) {
            colaEsperaRepository.saveAll(colasActualizadas);
        }
    }
}
