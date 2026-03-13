package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.ReservaSlotEntity;

import java.util.List;

@Repository
public interface ReservaSlotRepository extends JpaRepository<ReservaSlotEntity, Long> {
    List<ReservaSlotEntity> findAllByReservaId(Long reservaId);
    void deleteByReservaId(Long reservaId);
}
