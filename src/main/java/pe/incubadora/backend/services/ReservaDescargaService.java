package pe.incubadora.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.incubadora.backend.dtos.ReservaDTO;
import pe.incubadora.backend.entities.*;
import pe.incubadora.backend.repositories.*;
import pe.incubadora.backend.utils.CreateReservaDescargaResult;

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
        if (roles.contains("ROLE_TRANSPORTISTA")){
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
            return  CreateReservaDescargaResult.PESO_EXCEDE_MUELLE;
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
        List<Integer> duracionesValidas = List.of(30, 60, 90, 120);
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

    public List<LocalTime> calcularSlots(LocalTime horaInicio, int duracionMin) {
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
