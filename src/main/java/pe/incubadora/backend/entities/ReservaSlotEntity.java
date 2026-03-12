package pe.incubadora.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reserva_slots", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_muelle_fecha_hora",
        columnNames = {"muelle_id", "fecha", "hora_slot"}
    )}
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private LocalTime horaSlot;

    @ManyToOne
    @JoinColumn(name = "muelle_id")
    private MuelleEntity muelle;

    @ManyToOne
    @JoinColumn(name = "reserva_id")
    private ReservaDescargaEntity reserva;
}
