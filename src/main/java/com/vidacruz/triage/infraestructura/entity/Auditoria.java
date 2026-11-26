package com.vidacruz.triage.infraestructura.entity;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.TipoEvento;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("auditoria")
public record Auditoria(
        @Id Long id,
        UUID pacienteId,
        TipoEvento tipoEvento,
        EstadoPaciente estadoAnterior,
        EstadoPaciente estadoNuevo,
        OffsetDateTime fechaEvento) {
}