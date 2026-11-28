package com.vidacruz.triage.config;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorrelacionConfig {

    // Le enseña a Micrometer cómo copiar el correlationId entre el contexto de Reactor y el MDC
    @PostConstruct
    void registrar() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                CorrelacionFilter.CLAVE,
                () -> MDC.get(CorrelacionFilter.CLAVE),
                valor -> MDC.put(CorrelacionFilter.CLAVE, valor),
                () -> MDC.remove(CorrelacionFilter.CLAVE));
    }
}