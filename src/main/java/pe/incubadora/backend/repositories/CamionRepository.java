package pe.incubadora.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.CamionEntity;

import java.util.Optional;

@Repository
public interface CamionRepository extends JpaRepository<CamionEntity, Long> {
    boolean existsByIdAndEmpresaId(Long id, Long empresaId);
    Optional<CamionEntity> findByIdAndActivoTrue(Long id);
}
