package com.vidacruz.triage.domain;

import com.vidacruz.triage.domain.AlertaEscalamiento;
import com.vidacruz.triage.domain.enums.NivelTriage;
import com.vidacruz.triage.infraestructura.alertas.NotificadorWebhook;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificadorWebhookTest {

    @Test
    void elMensajeIncluyeIdYNivelPeroNoDatosPersonales() {
        UUID id = UUID.randomUUID();
        var alerta = new AlertaEscalamiento(id, NivelTriage.CRITICO, OffsetDateTime.now());

        String mensaje = NotificadorWebhook.construirMensaje(alerta);

        assertTrue(mensaje.contains(id.toString()));
        assertTrue(mensaje.contains("CRITICO"));
        assertTrue(mensaje.contains("GET /triage/pacientes/" + id));
    }
}