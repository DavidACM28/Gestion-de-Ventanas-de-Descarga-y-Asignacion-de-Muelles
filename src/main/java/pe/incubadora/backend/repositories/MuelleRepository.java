package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.MuelleEntity;

import java.util.Optional;

@Repository
public interface MuelleRepository extends JpaRepository<MuelleEntity, Long> {
    Optional<MuelleEntity> findByIdAndActivoTrue(Long id);
}
