package com.vidacruz.triage.config;


import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrelacionFilterTest {

    private final CorrelacionFilter filtro = new CorrelacionFilter();

    private String idDeRespuesta(MockServerHttpRequest.BaseBuilder<?> peticion) {
        MockServerWebExchange exchange = MockServerWebExchange.from(peticion);
        filtro.filter(exchange, e -> Mono.empty()).block();
        return exchange.getResponse().getHeaders().getFirst(CorrelacionFilter.HEADER);
    }

    @Test
    void siNoLlegaId_segeneraUno() {
        assertNotNull(idDeRespuesta(MockServerHttpRequest.get("/x")));
    }

    @Test
    void siLlegaUnIdValido_seRespeta() {
        String id = idDeRespuesta(MockServerHttpRequest.get("/x")
                .header(CorrelacionFilter.HEADER, "abc-12345-xyz"));
        assertEquals("abc-12345-xyz", id);
    }

    @Test
    void siLlegaUnIdConCaracteresRaros_seDescartaYSeGeneraOtro() {
        String id = idDeRespuesta(MockServerHttpRequest.get("/x")
                .header(CorrelacionFilter.HEADER, "malo\nINFO falso"));
        assertNotEquals("malo\nINFO falso", id);
    }
}