package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.TipoEvento;
import com.vidacruz.triage.infraestructura.entity.Auditoria;
import com.vidacruz.triage.infraestructura.entity.Paciente;
import com.vidacruz.triage.infraestructura.kafka.EventoPaciente;
import com.vidacruz.triage.infraestructura.kafka.PublicadorEventos;
import com.vidacruz.triage.infraestructura.repository.AuditoriaRepository;
import com.vidacruz.triage.infraestructura.repository.PacienteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class EscalamientoService {

    private static final Logger log = LoggerFactory.getLogger(EscalamientoService.class);

    private final PacienteRepository repositorio;
    private final AuditoriaRepository auditoriaRepositorio;
    private final PublicadorEventos publicador;
    private final TransactionalOperator transaccion;
    private final MeterRegistry metricas;

    public EscalamientoService(PacienteRepository repositorio,
                               AuditoriaRepository auditoriaRepositorio,
                               PublicadorEventos publicador,
                               TransactionalOperator transaccion, MeterRegistry metricas) {
        this.repositorio = repositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.publicador = publicador;
        this.transaccion = transaccion;
        this.metricas = metricas;
    }

    // Un ciclo completo: devuelve cuántos pacientes se escalaron
    public Mono<Long> evaluar() {
        return repositorio.buscarVencidos()
                .concatMap(paciente -> escalar(paciente)
                        // Se cuenta después de la transacción, y solo si de verdad se escaló
                        .doOnNext(escalado -> {
                            if (escalado) {
                                metricas.counter("triage.escalamientos", "nivel", paciente.nivel().name())
                                        .increment();
                            }
                        })
                        .onErrorResume(error -> {
                            log.error("No se pudo escalar al paciente {}", paciente.id(), error);
                            return Mono.just(false);
                        }))
                .filter(escalado -> escalado)
                .count();
    }

    // Escala a un paciente. Devuelve true si realmente lo escaló.
    private Mono<Boolean> escalar(Paciente paciente) {
        Mono<Boolean> pasos = repositorio.marcarEscalado(paciente.id())
                .filter(filasCambiadas -> filasCambiadas == 1)   // si fue 0, termina vacío
                .flatMap(cambio -> {
                    OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

                    Auditoria fila = new Auditoria(null, paciente.id(), TipoEvento.PACIENTE_ESCALADO,
                            EstadoPaciente.REGISTRADO, EstadoPaciente.REGISTRADO, ahora);
                    EventoPaciente evento = new EventoPaciente(
                            TipoEvento.PACIENTE_ESCALADO.nombreKafka(), paciente.id(), paciente.nivel(),
                            EstadoPaciente.REGISTRADO, EstadoPaciente.REGISTRADO, ahora);

                    return auditoriaRepositorio.save(fila)
                            .then(publicador.publicar(evento))
                            .thenReturn(true);
                })
                .defaultIfEmpty(false);

        return transaccion.transactional(pasos);
    }
}