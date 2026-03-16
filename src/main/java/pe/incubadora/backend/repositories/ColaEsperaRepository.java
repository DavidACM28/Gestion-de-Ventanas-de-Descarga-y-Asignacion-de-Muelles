package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.ColaEsperaEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ColaEsperaRepository extends JpaRepository<ColaEsperaEntity, Long>, JpaSpecificationExecutor<ColaEsperaEntity> {

    boolean existsByCamionIdAndFechaAndEstado(Long camionId, LocalDate fecha, String estado);

    List<ColaEsperaEntity> findByFechaAndTipoCargaAndEstado(LocalDate fecha, String tipoCarga, String estado);
}
