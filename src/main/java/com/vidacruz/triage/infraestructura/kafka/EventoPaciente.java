package com.vidacruz.triage.infraestructura.kafka;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoPaciente(
        String tipo,
        UUID pacienteId,
        NivelTriage nivel,
        EstadoPaciente estadoAnterior,
        EstadoPaciente estadoNuevo,
        OffsetDateTime timestamp) {
}