package com.vidacruz.triage.infraestructura.dto;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PacienteResponse(
        UUID id,
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
        OffsetDateTime fechaEstadoActual,
        long segundosEnEstadoActual) {
}
