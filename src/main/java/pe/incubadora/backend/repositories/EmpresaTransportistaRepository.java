package pe.incubadora.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;

@Repository
public interface EmpresaTransportistaRepository extends JpaRepository<EmpresaTransportistaEntity, Long> {
}
