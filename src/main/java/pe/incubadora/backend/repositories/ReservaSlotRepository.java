package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.ReservaSlotEntity;

@Repository
public interface ReservaSlotRepository extends JpaRepository<ReservaSlotEntity, Long> {
}
