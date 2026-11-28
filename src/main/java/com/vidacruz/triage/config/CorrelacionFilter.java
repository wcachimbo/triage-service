package com.vidacruz.triage.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

// Va primero de todos, para que incluso las respuestas 401 lleven el header
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacionFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CLAVE = "correlationId";

    // Solo letras, números y guiones: evita que alguien meta saltos de línea para falsificar logs
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String recibido = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = (recibido != null && VALIDO.matcher(recibido).matches())
                ? recibido
                : UUID.randomUUID().toString();

        exchange.getResponse().getHeaders().set(HEADER, id);

        return chain.filter(exchange)
                .contextWrite(contexto -> contexto.put(CLAVE, id));
    }
}