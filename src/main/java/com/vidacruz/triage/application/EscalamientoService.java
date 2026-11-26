package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.TipoEvento;
import com.vidacruz.triage.infraestructura.entity.Auditoria;
import com.vidacruz.triage.infraestructura.entity.Paciente;
import com.vidacruz.triage.infraestructura.kafka.EventoPaciente;
import com.vidacruz.triage.infraestructura.kafka.PublicadorEventos;
import com.vidacruz.triage.infraestructura.repository.AuditoriaRepository;
import com.vidacruz.triage.infraestructura.repository.PacienteRepository;
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

    public EscalamientoService(PacienteRepository repositorio,
                               AuditoriaRepository auditoriaRepositorio,
                               PublicadorEventos publicador,
                               TransactionalOperator transaccion) {
        this.repositorio = repositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.publicador = publicador;
        this.transaccion = transaccion;
    }

    // Un ciclo completo: devuelve cuántos pacientes se escalaron
    public Mono<Long> evaluar() {
        return repositorio.buscarVencidos()
                .concatMap(paciente -> escalar(paciente)
                        // Si falla uno, se registra y se sigue con los demás
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

                    // El estado no cambia (sigue REGISTRADO): lo que cambia es el flag
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

        // Todo o nada: marcar + auditar + publicar en una sola transacción
        return transaccion.transactional(pasos);
    }
}