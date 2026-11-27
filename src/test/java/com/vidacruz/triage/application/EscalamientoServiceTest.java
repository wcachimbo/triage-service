package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;
import com.vidacruz.triage.domain.enums.TipoEvento;
import com.vidacruz.triage.infraestructura.entity.Auditoria;
import com.vidacruz.triage.infraestructura.entity.Paciente;
import com.vidacruz.triage.infraestructura.kafka.PublicadorEventos;
import com.vidacruz.triage.infraestructura.repository.AuditoriaRepository;
import com.vidacruz.triage.infraestructura.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EscalamientoServiceTest {

    private final PacienteRepository repositorio = mock(PacienteRepository.class);
    private final AuditoriaRepository auditoria = mock(AuditoriaRepository.class);
    private final PublicadorEventos publicador = mock(PublicadorEventos.class);
    private final TransactionalOperator transaccion = mock(TransactionalOperator.class);

    private EscalamientoService servicio;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        when(transaccion.transactional(any(Mono.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(auditoria.save(any(Auditoria.class)))
                .thenAnswer(invocacion -> Mono.just(invocacion.getArgument(0)));

        servicio = new EscalamientoService(repositorio, auditoria, publicador, transaccion);
    }

    private static Paciente pacienteVencido() {
        OffsetDateTime hace30Min = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30);
        return new Paciente(UUID.randomUUID(), "Paciente de prueba", 40, 80, 120, 80, 98, 36.5,
                0, 15, "Dolor de cabeza", NivelTriage.URGENTE, 10, EstadoPaciente.REGISTRADO,
                false, hace30Min, null, hace30Min);
    }

    @Test
    void cuandoElUpdateCambiaUnaFila_auditaYPublicaUnaVez() {
        Paciente paciente = pacienteVencido();
        when(repositorio.buscarVencidos()).thenReturn(Flux.just(paciente));
        when(repositorio.marcarEscalado(paciente.id())).thenReturn(Mono.just(1));
        when(publicador.publicar(any())).thenReturn(Mono.empty());

        StepVerifier.create(servicio.evaluar())
                .expectNext(1L)
                .verifyComplete();

        verify(auditoria, times(1))
                .save(argThat(a -> a.tipoEvento() == TipoEvento.PACIENTE_ESCALADO));
        verify(publicador, times(1))
                .publicar(argThat(e -> e.tipo().equals("paciente_escalado")));
    }

    @Test
    void cuandoElUpdateNoCambiaFilas_noAuditaNiPublica() {
        Paciente paciente = pacienteVencido();
        when(repositorio.buscarVencidos()).thenReturn(Flux.just(paciente));
        when(repositorio.marcarEscalado(paciente.id())).thenReturn(Mono.just(0));

        StepVerifier.create(servicio.evaluar())
                .expectNext(0L)
                .verifyComplete();

        verifyNoInteractions(auditoria, publicador);
    }

    @Test
    void dosCiclosSobreElMismoPaciente_soloElPrimeroPublica() {
        Paciente paciente = pacienteVencido();
        when(repositorio.buscarVencidos()).thenReturn(Flux.just(paciente));
        when(repositorio.marcarEscalado(paciente.id())).thenReturn(Mono.just(1), Mono.just(0));
        when(publicador.publicar(any())).thenReturn(Mono.empty());

        StepVerifier.create(servicio.evaluar()).expectNext(1L).verifyComplete();
        StepVerifier.create(servicio.evaluar()).expectNext(0L).verifyComplete();

        verify(auditoria, times(1)).save(any(Auditoria.class));
        verify(publicador, times(1)).publicar(any());
    }

    @Test
    void siFallaUnPaciente_seSigueConElSiguiente() {
        Paciente primero = pacienteVencido();
        Paciente segundo = pacienteVencido();
        when(repositorio.buscarVencidos()).thenReturn(Flux.just(primero, segundo));
        when(repositorio.marcarEscalado(any())).thenReturn(Mono.just(1));
        // El primer publicar falla (Kafka caído) y el segundo funciona
        when(publicador.publicar(any()))
                .thenReturn(Mono.error(new RuntimeException("Kafka caído")), Mono.empty());

        StepVerifier.create(servicio.evaluar())
                .expectNext(1L)
                .verifyComplete();
    }
}