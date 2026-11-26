package com.vidacruz.triage.infraestructura.entity;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("pacientes")
public record Paciente(
        @Id UUID id,
        String nombre,
        int edad,
        int frecuenciaCardiaca,
        int presionSistolica,
        int presionDiastolica,
        int spo2,
        double temperatura,
        int dolor,
        int gcs,
        String quejaPrincipal,
        NivelTriage nivel,
        int slaMinutos,
        EstadoPaciente estado,
        boolean escalado,
        OffsetDateTime fechaRegistro,
        OffsetDateTime fechaEscalado,
        OffsetDateTime fechaEstadoActual) {
    // Devuelve una copia del paciente con el nuevo estado
    public Paciente conEstado(EstadoPaciente nuevoEstado, OffsetDateTime ahora) {
        return new Paciente(
                id, nombre, edad, frecuenciaCardiaca, presionSistolica, presionDiastolica,
                spo2, temperatura, dolor, gcs, quejaPrincipal, nivel, slaMinutos,
                nuevoEstado, escalado, fechaRegistro, fechaEscalado, ahora);
    }

}
