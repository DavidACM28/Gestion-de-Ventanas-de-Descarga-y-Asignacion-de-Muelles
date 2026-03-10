package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pe.incubadora.backend.utils.Rol;

import java.time.LocalDateTime;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "usuario")
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private EmpresaTransportistaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id", unique = true)
    private CamionEntity camion;

    @Column(name = "username", unique = true)
    private String username;

    private String password;
    private Rol rol;
    private boolean activo;

    @CreatedDate
    private LocalDateTime createdAt;

}
