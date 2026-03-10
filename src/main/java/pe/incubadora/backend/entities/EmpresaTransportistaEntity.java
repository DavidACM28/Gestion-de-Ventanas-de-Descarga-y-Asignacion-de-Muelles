package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "empresa_transportista")
public class EmpresaTransportistaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String ruc;
    private String razonSocial;
    private String contactoNombre;
    private String contactoTelefono;
    private String email;
    private boolean activo;
}
