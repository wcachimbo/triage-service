package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.AlertaEscalamiento;
import reactor.core.publisher.Mono;

public interface Notificador {

    Mono<Void> alertar(AlertaEscalamiento alerta);
}
