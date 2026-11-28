package com.vidacruz.triage.infraestructura.scheduler;

import com.vidacruz.triage.application.EscalamientoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class ProcesoEscalamiento {

    private static final Logger log = LoggerFactory.getLogger(ProcesoEscalamiento.class);

    private final EscalamientoService servicio;

    public ProcesoEscalamiento(EscalamientoService servicio) {
        this.servicio = servicio;
    }


    @Scheduled(fixedDelayString = "${triage.escalamiento.intervalo-segundos}", timeUnit = TimeUnit.SECONDS)
    public Mono<Void> ejecutar() {
        return servicio.evaluar()
                .doOnNext(cantidad -> {
                    if (cantidad > 0) {
                        log.info("Ciclo de escalamiento: {} paciente(s) escalado(s)", cantidad);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Falló el ciclo de escalamiento", error);
                    return Mono.empty();
                })
                .contextWrite(Context.of("correlationId", "escalamiento-" + UUID.randomUUID()))
                .then();

    }
}
