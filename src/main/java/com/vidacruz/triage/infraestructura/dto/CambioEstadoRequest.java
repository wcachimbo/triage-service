package com.vidacruz.triage.infraestructura.dto;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(
        @NotNull(message = "estado es obligatorio")
        EstadoPaciente estado) {
}