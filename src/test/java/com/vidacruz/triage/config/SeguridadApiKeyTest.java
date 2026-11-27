package com.vidacruz.triage.config;

import com.vidacruz.triage.application.PacienteService;
import com.vidacruz.triage.domain.exceptions.PacienteNoEncontradoException;
import com.vidacruz.triage.infraestructura.controller.ManejadorErrores;
import com.vidacruz.triage.infraestructura.controller.PacienteController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(PacienteController.class)
@Import({SecurityConfig.class, ManejadorErrores.class})
@TestPropertySource(properties = "triage.security.api-key=clave-de-test")
class SeguridadApiKeyTest {

    @Autowired
    private WebTestClient cliente;

    @MockitoBean
    private PacienteService servicio;

    private final UUID id = UUID.randomUUID();

    @Test
    void sinApiKey_responde401() {
        cliente.get().uri("/triage/pacientes/" + id)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void conApiKeyIncorrecta_responde401() {
        cliente.get().uri("/triage/pacientes/" + id)
                .header("X-API-KEY", "otra-clave")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void conApiKeyCorrecta_pasaLaSeguridad() {
        when(servicio.obtener(id)).thenReturn(Mono.error(new PacienteNoEncontradoException(id)));

        cliente.get().uri("/triage/pacientes/" + id)
                .header("X-API-KEY", "clave-de-test")
                .exchange()
                .expectStatus().isNotFound();
    }
}