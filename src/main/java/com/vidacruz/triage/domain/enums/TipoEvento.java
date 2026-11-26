package com.vidacruz.triage.domain.enums;

import java.util.Locale;

public enum TipoEvento {
    PACIENTE_REGISTRADO,
    PACIENTE_EN_CONSULTA,
    PACIENTE_ESCALADO,
    PACIENTE_ALTA;

    // Nombre del evento tal como se publica en Kafka
    public String nombreKafka() {
        return name().toLowerCase(Locale.ROOT);
    }
}