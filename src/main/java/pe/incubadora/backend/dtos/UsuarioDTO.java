package pe.incubadora.backend.dtos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import pe.incubadora.backend.entities.CamionEntity;
import pe.incubadora.backend.entities.EmpresaTransportistaEntity;
import pe.incubadora.backend.utils.Rol;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {
    private Long id;
    private EmpresaTransportistaEntity empresa;
    private CamionEntity camion;
    private String username;
    private Rol rol;
    private boolean activo;
}
