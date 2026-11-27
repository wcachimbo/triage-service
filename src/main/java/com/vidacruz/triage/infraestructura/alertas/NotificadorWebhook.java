package com.vidacruz.triage.infraestructura.alertas;

import com.vidacruz.triage.application.Notificador;
import com.vidacruz.triage.domain.AlertaEscalamiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
public class NotificadorWebhook implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorWebhook.class);

    private final WebClient cliente;
    private final String webhookUrl;

    public NotificadorWebhook(WebClient.Builder builder,
                              @Value("${triage.alertas.webhook-url:}") String webhookUrl) {
        this.cliente = builder.build();
        this.webhookUrl = webhookUrl;
    }

    @Override
    public Mono<Void> alertar(AlertaEscalamiento alerta) {

        log.warn("ALERTA_ESCALAMIENTO pacienteId={} nivel={}", alerta.pacienteId(), alerta.nivel());

        if (webhookUrl.isBlank()) {
            return Mono.empty();
        }

        return cliente.post()
                .uri(webhookUrl)
                .bodyValue(Map.of("text", construirMensaje(alerta)))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .doOnSuccess(r -> log.info("Alerta enviada al webhook para el paciente {}", alerta.pacienteId()))
                .onErrorResume(error -> {
                    // Si el webhook falla, no tumbamos nada: ya quedó en el log
                    log.error("No se pudo enviar la alerta al webhook para el paciente {}",
                            alerta.pacienteId(), error);
                    return Mono.empty();
                })
                .then();
    }

    public static String construirMensaje(AlertaEscalamiento alerta) {
        return "ALERTA: paciente escalado por superar su tiempo máximo de espera. "
                + "Nivel: " + alerta.nivel() + ". "
                + "Id: " + alerta.pacienteId() + ". "
                + "Consultar: GET /triage/pacientes/" + alerta.pacienteId();
    }
}