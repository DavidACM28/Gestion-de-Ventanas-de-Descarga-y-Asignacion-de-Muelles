package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.ReservaDescargaEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaDescargaRepository extends JpaRepository<ReservaDescargaEntity, Long>, JpaSpecificationExecutor<ReservaDescargaEntity> {


    @Query(
        "SELECT r FROM ReservaDescargaEntity r WHERE  r.fecha = :fecha AND r.muelle.id = :muelleId AND " +
        "(r.estado = 'SOLICITADA' OR r.estado = 'CONFIRMADA' OR r.estado = 'CHECK_IN' OR r.estado = 'EN_DESCARGA')")
    List<ReservaDescargaEntity> findAllByFechaAndMuelleIdAndEstado(LocalDate fecha, Long muelleId);
}
