package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.MuelleEntity;

@Repository
public interface MuelleRepository extends JpaRepository<MuelleEntity, Long> {
}
