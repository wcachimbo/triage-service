package com.vidacruz.triage.domain;

import com.vidacruz.triage.domain.enums.NivelTriage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertaEscalamiento(
        UUID pacienteId,
        NivelTriage nivel,
        OffsetDateTime momento) {
}