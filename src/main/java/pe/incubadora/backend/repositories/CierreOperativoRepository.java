package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.CierreOperativoEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CierreOperativoRepository extends JpaRepository<CierreOperativoEntity, Long>, JpaSpecificationExecutor<CierreOperativoEntity> {

    @Query("SELECT c FROM CierreOperativoEntity c WHERE c.muelle.id = :muelleId AND c.fecha = :fecha AND c.horaInicio < :finNueva AND c.horaFin > :inicioNueva")
    List<CierreOperativoEntity> findCierresOperativosEnRango(Long muelleId, LocalDate fecha, LocalTime finNueva, LocalTime inicioNueva);

}
