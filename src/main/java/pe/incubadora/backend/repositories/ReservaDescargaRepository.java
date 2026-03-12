package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.ReservaDescargaEntity;

@Repository
public interface ReservaDescargaRepository extends JpaRepository<ReservaDescargaEntity, Long> {
}
