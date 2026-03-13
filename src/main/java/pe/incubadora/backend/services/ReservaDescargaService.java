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
import pe.incubadora.backend.dtos.ReservaDTO;
import pe.incubadora.backend.entities.*;
import pe.incubadora.backend.repositories.*;
import pe.incubadora.backend.utils.CambiarEstadoReservaResult;
import pe.incubadora.backend.utils.CreateReservaDescargaResult;
import pe.incubadora.backend.utils.UpdateReservaResult;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservaDescargaService {
    @Autowired
    private ReservaDescargaRepository reservaDescargaRepository;
    @Autowired
    private ReservaSlotRepository reservaSlotRepository;
    @Autowired
    private MuelleRepository muelleRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private CierreOperativoRepository cierreOperativoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private final List<Integer> duracionesValidas = List.of(30, 60, 90, 120);

    @Transactional
    public CreateReservaDescargaResult crearReserva(ReservaDTO reservaDTO) {
        LocalTime horaInicio;
        LocalDate fecha;
        LocalDateTime fechaYHoraInicio;
        MuelleEntity muelleEntity = muelleRepository.findByIdAndActivoTrue(reservaDTO.getMuelleId()).orElse(null);
        if (muelleEntity == null) {
            return CreateReservaDescargaResult.MUELLE_NOT_FOUND;
        }
        CamionEntity camionEntity = camionRepository.findByIdAndActivoTrue(reservaDTO.getCamionId()).orElse(null);
        if (camionEntity == null) {
            return CreateReservaDescargaResult.CAMION_NOT_FOUND;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roles.contains("ROLE_TRANSPORTISTA")) {
            UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
            assert usuario != null;
            if (!usuario.getCamion().getId().equals(camionEntity.getId())) {
                return CreateReservaDescargaResult.CAMION_NOT_FOUND;
            }
        }
        if (!muelleEntity.getTipoCargaPermitida().equals(camionEntity.getTipoCarga())) {
            return CreateReservaDescargaResult.TIPO_CARGA_INVALIDA;
        }
        if (reservaDTO.getPesoEstimadoToneladas().compareTo(muelleEntity.getCapacidadToneladas()) > 0) {
            return CreateReservaDescargaResult.PESO_EXCEDE_MUELLE;
        }
        try {
            fecha = LocalDate.parse(reservaDTO.getFecha());
        } catch (DateTimeParseException e) {
            return CreateReservaDescargaResult.FECHA_INVALIDA;
        }
        try {
            horaInicio = LocalTime.parse(reservaDTO.getHoraInicio());
        } catch (DateTimeParseException e) {
            return CreateReservaDescargaResult.HORA_INVALIDA;
        }
        fechaYHoraInicio = LocalDateTime.of(fecha, horaInicio);
        if (fechaYHoraInicio.isBefore(LocalDateTime.now())) {
            return CreateReservaDescargaResult.FECHA_PASADA;
        }
        if (!duracionesValidas.contains(reservaDTO.getDuracionMin())) {
            return CreateReservaDescargaResult.DURACION_INVALIDA;
        }
        LocalTime finDescarga = horaInicio.plusMinutes(reservaDTO.getDuracionMin());
        List<CierreOperativoEntity> cierresOperativosEnRango = cierreOperativoRepository.findCierresOperativosEnRango(
            reservaDTO.getMuelleId(), fecha, finDescarga, horaInicio);
        if (!cierresOperativosEnRango.isEmpty()) {
            return CreateReservaDescargaResult.CIERRE_CONFLICT;
        }

        List<LocalTime> slots = calcularSlots(horaInicio, reservaDTO.getDuracionMin());
        ReservaDescargaEntity reservaDescargaEntity = new ReservaDescargaEntity();
        reservaDescargaEntity.setDuracionMin(reservaDTO.getDuracionMin());
        reservaDescargaEntity.setEstado("SOLICITADA");
        reservaDescargaEntity.setFecha(fecha);
        reservaDescargaEntity.setHoraInicio(horaInicio);
        reservaDescargaEntity.setNota(reservaDTO.getNota());
        reservaDescargaEntity.setPesoEstimadoToneladas(reservaDTO.getPesoEstimadoToneladas());
        reservaDescargaEntity.setTipoMercaderia(reservaDTO.getTipoMercaderia());
        reservaDescargaEntity.setCamion(camionEntity);
        reservaDescargaEntity.setMuelle(muelleEntity);
        reservaDescargaRepository.save(reservaDescargaEntity);

        List<ReservaSlotEntity> slotsEntities = new ArrayList<>();
        for (LocalTime slot : slots) {
            ReservaSlotEntity reservaSlotEntity = new ReservaSlotEntity();
            reservaSlotEntity.setFecha(fecha);
            reservaSlotEntity.setHoraSlot(slot);
            reservaSlotEntity.setMuelle(muelleEntity);
            reservaSlotEntity.setReserva(reservaDescargaEntity);
            slotsEntities.add(reservaSlotEntity);
        }
        reservaSlotRepository.saveAll(slotsEntities);
        return CreateReservaDescargaResult.CREATED;
    }

    @Transactional
    public UpdateReservaResult updateReserva(ReservaDTO reservaDTO, Long id) {
        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);
        if (reserva == null) {
            return UpdateReservaResult.RESERVA_NOT_FOUND;
        }
        UpdateReservaResult resultado = validarUpdateReserva(reservaDTO, reserva);
        if (resultado != null) {
            return resultado;
        }

        LocalDate fecha = reserva.getFecha();
        LocalTime horaInicio = reserva.getHoraInicio();
        int duracion = reserva.getDuracionMin();
        MuelleEntity muelle = reserva.getMuelle();

        if (reservaDTO.getFecha() != null) {
            fecha = LocalDate.parse(reservaDTO.getFecha());
        }
        if (reservaDTO.getHoraInicio() != null) {
            horaInicio = LocalTime.parse(reservaDTO.getHoraInicio());
        }
        if (reservaDTO.getDuracionMin() != null) {
            duracion = reservaDTO.getDuracionMin();
        }
        if (reservaDTO.getMuelleId() != null) {
            muelle = muelleRepository.findById(reservaDTO.getMuelleId()).orElse(null);
        }
        if (muelle == null) {
            return UpdateReservaResult.MUELLE_NOT_FOUND;
        }

        LocalTime finDescarga = horaInicio.plusMinutes(duracion);
        List<CierreOperativoEntity> cierresOperativosEnRango = cierreOperativoRepository.findCierresOperativosEnRango(
            muelle.getId(), fecha, finDescarga, horaInicio);
        if (!cierresOperativosEnRango.isEmpty()) {
            return UpdateReservaResult.CIERRE_CONFLICT;
        }

        List<ReservaSlotEntity> slotsActuales = reservaSlotRepository.findAllByReservaId(reserva.getId());
        reservaSlotRepository.deleteAll(slotsActuales);

        List<LocalTime> slotsNuevos = calcularSlots(horaInicio, duracion);

        for (LocalTime slot : slotsNuevos) {
            ReservaSlotEntity nuevoSlot = new ReservaSlotEntity();
            nuevoSlot.setHoraSlot(slot);
            nuevoSlot.setFecha(fecha);
            nuevoSlot.setReserva(reserva);
            nuevoSlot.setMuelle(muelle);

            reservaSlotRepository.save(nuevoSlot);
        }
        aplicarCambios(reservaDTO, reserva);
        reservaDescargaRepository.save(reserva);
        return UpdateReservaResult.UPDATED;
    }

    public ReservaDescargaEntity getReserva(Long id) {
        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);
        if (reserva == null) {
            return null;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roles.contains("ROLE_TRANSPORTISTA")) {
            UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
            assert usuario != null;
            if (!usuario.getCamion().getId().equals(reserva.getCamion().getId())) {
                return null;
            }
        }
        return reserva;
    }

    public Page<ReservaDescargaEntity> getReservasConFiltros(
        Long muelleId, Long camionId, Long empresaId, LocalDate fechaDesde, LocalDate fechaHasta,
        String estado, String tipoCarga, int page, int size, String sort) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        assert usuario != null;

        Specification<ReservaDescargaEntity> spec = Specification.where((_, _, cb) -> cb.conjunction());

        if (roles.contains("ROLE_TRANSPORTISTA")) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("camion").get("id"), usuario.getCamion().getId()));
        }
        if (muelleId != null) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("muelle").get("id"), muelleId));
        }
        if (camionId != null) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("camion").get("id"), camionId));
        }
        if (empresaId != null) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("camion").get("empresa").get("id"), empresaId));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, _, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, _, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }
        if (estado != null) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("estado"), estado.toUpperCase()));
        }
        if (tipoCarga != null) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("muelle").get("tipoCargaPermitida"), tipoCarga.toUpperCase()));
        }

        Sort.Direction direction = "descending".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "id"));
        return reservaDescargaRepository.findAll(spec, pageable);
    }

    public CambiarEstadoReservaResult confirmarReserva(Long id) {

        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);
        if (reserva == null) {
            return CambiarEstadoReservaResult.RESERVA_NOT_FOUND;
        }
        if (!reserva.getEstado().equals("SOLICITADA")) {
            return CambiarEstadoReservaResult.ESTADO_INVALIDO;
        }
        reserva.setEstado("CONFIRMADA");
        reservaDescargaRepository.save(reserva);
        return CambiarEstadoReservaResult.OK;
    }

    public CambiarEstadoReservaResult checkInReserva(Long id) {
        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);

        if (reserva == null) {
            return CambiarEstadoReservaResult.RESERVA_NOT_FOUND;
        }
        if (!reserva.getEstado().equals("CONFIRMADA")) {
            return CambiarEstadoReservaResult.ESTADO_INVALIDO;
        }
        LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        LocalDateTime inicioVentana = inicio.minusMinutes(30);
        LocalDateTime finVentana = inicio.plusMinutes(20);
        LocalDateTime ahora = LocalDateTime.now();

        if (ahora.isBefore(inicioVentana) || ahora.isAfter(finVentana)) {
            return CambiarEstadoReservaResult.FUERA_DE_VENTANA;
        }
        reserva.setEstado("CHECK_IN");
        reservaDescargaRepository.save(reserva);
        return CambiarEstadoReservaResult.OK;
    }

    public CambiarEstadoReservaResult iniciarReserva(Long id) {
        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);

        if (reserva == null) {
            return CambiarEstadoReservaResult.RESERVA_NOT_FOUND;
        }
        if (!reserva.getEstado().equals("CHECK_IN")) {
            return CambiarEstadoReservaResult.ESTADO_INVALIDO;
        }
        reserva.setEstado("EN_DESCARGA");
        reservaDescargaRepository.save(reserva);
        return CambiarEstadoReservaResult.OK;
    }

    public CambiarEstadoReservaResult finalizarReserva(Long id) {

        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);

        if (reserva == null) {
            return CambiarEstadoReservaResult.RESERVA_NOT_FOUND;
        }
        if (!reserva.getEstado().equals("EN_DESCARGA")) {
            return CambiarEstadoReservaResult.ESTADO_INVALIDO;
        }
        reserva.setEstado("FINALIZADA");
        reservaDescargaRepository.save(reserva);
        return CambiarEstadoReservaResult.OK;
    }

    public CambiarEstadoReservaResult cancelarReserva(Long id) {

        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);

        if (reserva == null) {
            return CambiarEstadoReservaResult.RESERVA_NOT_FOUND;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        assert usuario != null;
        if (roles.contains("ROLE_TRANSPORTISTA")) {
            if (!reserva.getCamion().getId().equals(usuario.getCamion().getId())) {
                return  CambiarEstadoReservaResult.RESERVA_NOT_FOUND;
            }
            LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
            if (!inicio.isBefore(LocalDateTime.now().minusHours(3))) {
                return CambiarEstadoReservaResult.FUERA_DE_VENTANA;
            }
        }

        if (!reserva.getEstado().equals("SOLICITADA") && !reserva.getEstado().equals("CONFIRMADA")
            && !reserva.getEstado().equals("CHECK_IN")) {
            return CambiarEstadoReservaResult.ESTADO_INVALIDO;
        }
        reserva.setEstado("CANCELADA");
        reservaDescargaRepository.save(reserva);
        List<ReservaSlotEntity> reservas = reservaSlotRepository.findAllByReservaId(reserva.getId());
        reservaSlotRepository.deleteAll(reservas);
        return CambiarEstadoReservaResult.OK;
    }

    public CambiarEstadoReservaResult noShowReserva(Long id) {

        ReservaDescargaEntity reserva = reservaDescargaRepository.findById(id).orElse(null);

        if (reserva == null) {
            return CambiarEstadoReservaResult.RESERVA_NOT_FOUND;
        }
        if (!reserva.getEstado().equals("CONFIRMADA")) {
            return CambiarEstadoReservaResult.ESTADO_INVALIDO;
        }
        reserva.setEstado("NO_SHOW");
        reservaDescargaRepository.save(reserva);
        return CambiarEstadoReservaResult.OK;
    }

    private UpdateReservaResult validarUpdateReserva(ReservaDTO dto, ReservaDescargaEntity reserva) {
        MuelleEntity muelle = reserva.getMuelle();
        CamionEntity camion = reserva.getCamion();

        if (dto.getMuelleId() != null) {
            muelle = muelleRepository.findById(dto.getMuelleId()).orElse(null);
            if (muelle == null) return UpdateReservaResult.MUELLE_NOT_FOUND;
        }
        if (dto.getCamionId() != null) {
            camion = camionRepository.findById(dto.getCamionId()).orElse(null);
            if (camion == null) return UpdateReservaResult.CAMION_NOT_FOUND;
        }
        if (!muelle.getTipoCargaPermitida().equals(camion.getTipoCarga())) {
            return UpdateReservaResult.NO_COINCIDEN;
        }

        LocalDate fechaFinal = reserva.getFecha();
        LocalTime horaFinal = reserva.getHoraInicio();
        if (dto.getFecha() != null) {
            try {
                fechaFinal = LocalDate.parse(dto.getFecha());
            } catch (DateTimeException e) {
                return UpdateReservaResult.FECHA_INVALIDA;
            }
        }
        if (dto.getHoraInicio() != null) {
            try {
                horaFinal = LocalTime.parse(dto.getHoraInicio());
            } catch (DateTimeException e) {
                return UpdateReservaResult.HORA_INVALIDA;
            }
        }
        if (LocalDateTime.of(fechaFinal, horaFinal).isBefore(LocalDateTime.now())) {
            return UpdateReservaResult.FECHA_PASADA;
        }
        if (LocalDateTime.of(fechaFinal, horaFinal).isBefore(LocalDateTime.now().plusMinutes(30))) {
            return UpdateReservaResult.HORA_MUY_CERCANA;
        }
        if (dto.getDuracionMin() != null && !duracionesValidas.contains(dto.getDuracionMin())) {
            return UpdateReservaResult.DURACION_INVALIDA;
        }
        if (dto.getPesoEstimadoToneladas() != null) {
            if (dto.getPesoEstimadoToneladas().compareTo(muelle.getCapacidadToneladas()) > 0) {
                return UpdateReservaResult.PESO_EXCEDE;
            }
        }
        if (dto.getTipoMercaderia() != null && dto.getTipoMercaderia().trim().length() < 3) {
            return UpdateReservaResult.TIPO_MERCADERIA_INVALIDO;
        }
        return null;
    }

    private void aplicarCambios(ReservaDTO dto, ReservaDescargaEntity reserva) {
        if (dto.getMuelleId() != null) {
            reserva.setMuelle(muelleRepository.findById(dto.getMuelleId()).orElse(null));
        }
        if (dto.getCamionId() != null) {
            reserva.setCamion(camionRepository.findById(dto.getCamionId()).orElse(null));
        }
        if (dto.getFecha() != null) {
            reserva.setFecha(LocalDate.parse(dto.getFecha()));
        }
        if (dto.getHoraInicio() != null) {
            reserva.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
        }
        if (dto.getDuracionMin() != null) {
            reserva.setDuracionMin(dto.getDuracionMin());
        }
        if (dto.getPesoEstimadoToneladas() != null) {
            reserva.setPesoEstimadoToneladas(dto.getPesoEstimadoToneladas());
        }
        if (dto.getTipoMercaderia() != null) {
            reserva.setTipoMercaderia(dto.getTipoMercaderia());
        }
        if (dto.getNota() != null) {
            reserva.setNota(dto.getNota());
        }
    }

    private List<LocalTime> calcularSlots(LocalTime horaInicio, int duracionMin) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime horaFin = horaInicio.plusMinutes(duracionMin);
        LocalTime slotActual = LocalTime.of(horaInicio.getHour(), (horaInicio.getMinute() / 30) * 30);

        while (slotActual.isBefore(horaFin)) {
            LocalTime finSlot = slotActual.plusMinutes(30);
            if (finSlot.isAfter(horaInicio)) {
                slots.add(slotActual);
            }

            slotActual = slotActual.plusMinutes(30);
        }

        return slots;
    }
}
