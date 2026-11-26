package com.vidacruz.triage.infraestructura.dto;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistroResponse(
        UUID id,
        NivelTriage nivel,
        int slaMinutos,
        EstadoPaciente estado,
        OffsetDateTime fechaRegistro) {
}