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
import pe.incubadora.backend.dtos.CierreOperativoDTO;
import pe.incubadora.backend.entities.CierreOperativoEntity;
import pe.incubadora.backend.entities.MuelleEntity;
import pe.incubadora.backend.entities.ReservaDescargaEntity;
import pe.incubadora.backend.entities.UsuarioEntity;
import pe.incubadora.backend.repositories.CierreOperativoRepository;
import pe.incubadora.backend.repositories.MuelleRepository;
import pe.incubadora.backend.repositories.ReservaDescargaRepository;
import pe.incubadora.backend.repositories.ReservaSlotRepository;
import pe.incubadora.backend.utils.CreateCierreOperativoResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles operational closure business rules.
 *
 * <p>Closures block new reservations, may cancel affected reservations when
 * forced by administrators, and support paginated queries.</p>
 */
@Service
public class CierreOperativoService {
    @Autowired
    private CierreOperativoRepository cierreOperativoRepository;
    @Autowired
    private MuelleRepository muelleRepository;
    @Autowired
    private ReservaDescargaRepository reservaDescargaRepository;
    @Autowired
    private ReservaSlotRepository reservaSlotRepository;

    /**
     * Creates an operational closure after validating schedule conflicts.
     *
     * @param dto closure payload
     * @return closure creation result
     */
    @Transactional
    public CreateCierreOperativoResult createCierreOperativo(CierreOperativoDTO dto) {

        MuelleEntity muelle = muelleRepository.findById(dto.getMuelleId()).orElse(null);
        if (muelle == null) {
            return CreateCierreOperativoResult.MUELLE_NOT_FOUND;
        }

        LocalDate fecha;
        LocalTime horaInicio;
        LocalTime horaFin;

        try {
            fecha = LocalDate.parse(dto.getFecha());
        } catch (DateTimeParseException e) {
            return CreateCierreOperativoResult.FECHA_INVALIDA;
        }

        try {
            horaInicio = LocalTime.parse(dto.getHoraInicio());
            horaFin = LocalTime.parse(dto.getHoraFin());
        } catch (DateTimeParseException e) {
            return CreateCierreOperativoResult.HORA_INVALIDA;
        }

        if (!horaFin.isAfter(horaInicio)) {
            return CreateCierreOperativoResult.HORA_INVALIDA;
        }

        if (dto.getTipo() != null) {
            if (!dto.getTipo().equalsIgnoreCase("LIMPIEZA") &&
                !dto.getTipo().equalsIgnoreCase("MANTENIMIENTO") &&
                !dto.getTipo().equalsIgnoreCase("INSPECCION") &&
                !dto.getTipo().equalsIgnoreCase("EMERGENCIA")) {
                return CreateCierreOperativoResult.TIPO_INVALIDO;
            }
        }

        List<CierreOperativoEntity> cierresEnRango =
            cierreOperativoRepository.findCierresOperativosEnRango(
                muelle.getId(), fecha, horaFin, horaInicio
            );

        if (!cierresEnRango.isEmpty()) {
            return CreateCierreOperativoResult.CIERRE_CONFLICT;
        }

        List<ReservaDescargaEntity> reservas =
            reservaDescargaRepository.findAllByFechaAndMuelleIdAndEstado(
                fecha, muelle.getId()
            );

        List<ReservaDescargaEntity> reservasAfectadas = new ArrayList<>();

        for (ReservaDescargaEntity reserva : reservas) {

            LocalTime inicioReserva = reserva.getHoraInicio();
            LocalTime finReserva = inicioReserva.plusMinutes(reserva.getDuracionMin());

            if (horaInicio.isBefore(finReserva) && horaFin.isAfter(inicioReserva)) {
                reservasAfectadas.add(reserva);
            }
        }

        if (!reservasAfectadas.isEmpty()) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assert auth != null;
            boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

            for (ReservaDescargaEntity reserva : reservasAfectadas) {
                if (reserva.getEstado().equalsIgnoreCase("EN_DESCARGA")) {
                    return CreateCierreOperativoResult.EN_DESCARGA;
                }
            }

            if (!dto.getForce() || !isAdmin) {
                return CreateCierreOperativoResult.RESERVA_CONFLICT;
            }

            for (ReservaDescargaEntity reserva : reservasAfectadas) {
                reserva.setEstado("CANCELADA");
                reserva.setNota("Cancelada automáticamente por cierre operativo");
                reservaSlotRepository.deleteByReservaId(reserva.getId());
            }

            reservaDescargaRepository.saveAll(reservasAfectadas);
        }

        CierreOperativoEntity cierre = new CierreOperativoEntity();
        cierre.setMuelle(muelle);
        cierre.setFecha(fecha);
        cierre.setHoraInicio(horaInicio);
        cierre.setHoraFin(horaFin);
        cierre.setMotivo(dto.getMotivo());
        cierre.setTipo(dto.getTipo());

        cierreOperativoRepository.save(cierre);

        return CreateCierreOperativoResult.CREATED;
    }

    /**
     * Deletes a closure by identifier.
     *
     * @param id closure identifier
     * @return {@code true} when the closure existed and was removed
     */
    public boolean deleteCierreOperativo(Long id) {

        CierreOperativoEntity cierre = cierreOperativoRepository.findById(id).orElse(null);
        if (cierre == null) {
            return false;
        }
        cierreOperativoRepository.delete(cierre);
        return true;
    }

    /**
     * Returns closures using optional filters and pagination.
     *
     * @param muelleId pier filter
     * @param fechaDesde lower date bound
     * @param fechaHasta upper date bound
     * @param tipo closure type filter
     * @param page zero-based page number
     * @param size page size
     * @param sort sort direction keyword
     * @return paginated closure data
     */
    public Page<CierreOperativoEntity> getCierresConFiltros(
        Long muelleId,LocalDate fechaDesde, LocalDate fechaHasta,
        String tipo, int page, int size, String sort) {

        Specification<CierreOperativoEntity> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (muelleId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("muelle").get("id"), muelleId));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }
        if (tipo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo.toUpperCase()));
        }

        Sort.Direction direction = "descending".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "id"));
        return cierreOperativoRepository.findAll(spec, pageable);
    }

}
