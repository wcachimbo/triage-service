package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.Clasificador;
import com.vidacruz.triage.domain.MaquinaEstados;
import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;
import com.vidacruz.triage.domain.enums.TipoEvento;
import com.vidacruz.triage.domain.exceptions.PacienteNoEncontradoException;
import com.vidacruz.triage.domain.exceptions.TransicionInvalidaException;
import com.vidacruz.triage.infraestructura.dto.PacienteResponse;
import com.vidacruz.triage.infraestructura.dto.RegistroRequest;
import com.vidacruz.triage.infraestructura.dto.RegistroResponse;
import com.vidacruz.triage.infraestructura.entity.Auditoria;
import com.vidacruz.triage.infraestructura.entity.Paciente;
import com.vidacruz.triage.infraestructura.repository.AuditoriaRepository;
import com.vidacruz.triage.infraestructura.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class PacienteService {

    private final PacienteRepository repositorio;
    private final AuditoriaRepository auditoriaRepositorio;
    private final Clasificador clasificador = new Clasificador();
    private final MaquinaEstados maquinaEstados = new MaquinaEstados();

    // Los minutos de SLA vienen de application.properties
    private final int slaCritico;
    private final int slaUrgente;
    private final int slaUrgente2;
    private final int slaNoUrgente;
    private final int slaNoUrgente2;

    public PacienteService(PacienteRepository repositorio,
                           AuditoriaRepository auditoriaRepositorio,
                           @Value("${triage.sla.critico}") int slaCritico,
                           @Value("${triage.sla.urgente}") int slaUrgente,
                           @Value("${triage.sla.urgente-2}") int slaUrgente2,
                           @Value("${triage.sla.no-urgente}") int slaNoUrgente,
                           @Value("${triage.sla.no-urgente-2}") int slaNoUrgente2) {
        this.repositorio = repositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.slaCritico = slaCritico;
        this.slaUrgente = slaUrgente;
        this.slaUrgente2 = slaUrgente2;
        this.slaNoUrgente = slaNoUrgente;
        this.slaNoUrgente2 = slaNoUrgente2;
    }

    // ---------- POST: registrar ----------

    @Transactional
    public Mono<RegistroResponse> registrar(RegistroRequest req) {

        // 1. Clasificar
        NivelTriage nivel = clasificador.clasificar(
                req.frecuenciaCardiaca(), req.presionSistolica(), req.spo2(),
                req.temperatura(), req.dolor(), req.gcs(), req.quejaPrincipal());

        // 2. SLA según el nivel
        int sla = slaPara(nivel);

        // 3. Armar el paciente (id null: lo genera la base de datos)
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        Paciente nuevo = new Paciente(
                null, req.nombre(), req.edad(), req.frecuenciaCardiaca(),
                req.presionSistolica(), req.presionDiastolica(), req.spo2(),
                req.temperatura(), req.dolor(), req.gcs(), req.quejaPrincipal(),
                nivel, sla, EstadoPaciente.REGISTRADO, false,
                ahora, null, ahora);

        // 4. Guardar el paciente y, en la misma transacción, su auditoría
        return repositorio.save(nuevo)
                .flatMap(guardado -> auditoriaRepositorio
                        .save(new Auditoria(null, guardado.id(), TipoEvento.PACIENTE_REGISTRADO,
                                null, EstadoPaciente.REGISTRADO, ahora))
                        .thenReturn(guardado))
                .map(p -> new RegistroResponse(
                        p.id(), p.nivel(), p.slaMinutos(), p.estado(), p.fechaRegistro()));
    }

    // ---------- PATCH: cambiar estado ----------

    @Transactional
    public Mono<PacienteResponse> cambiarEstado(UUID id, EstadoPaciente nuevoEstado) {
        return repositorio.findById(id)
                .switchIfEmpty(Mono.error(new PacienteNoEncontradoException(id)))
                .flatMap(paciente -> {

                    // ¿Se puede pasar de este estado al nuevo?
                    if (!maquinaEstados.esValida(paciente.estado(), nuevoEstado)) {
                        return Mono.error(new TransicionInvalidaException(paciente.estado(), nuevoEstado));
                    }

                    OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
                    TipoEvento tipo = (nuevoEstado == EstadoPaciente.EN_CONSULTA)
                            ? TipoEvento.PACIENTE_EN_CONSULTA
                            : TipoEvento.PACIENTE_ALTA;

                    // Guardar el nuevo estado y, en la misma transacción, la auditoría
                    return repositorio.save(paciente.conEstado(nuevoEstado, ahora))
                            .flatMap(guardado -> auditoriaRepositorio
                                    .save(new Auditoria(null, guardado.id(), tipo,
                                            paciente.estado(), nuevoEstado, ahora))
                                    .thenReturn(guardado));
                })
                .map(this::aRespuesta);
    }

    // ---------- GET: obtener ----------

    public Mono<PacienteResponse> obtener(UUID id) {
        return repositorio.findById(id)
                .switchIfEmpty(Mono.error(new PacienteNoEncontradoException(id)))
                .map(this::aRespuesta);
    }

    // ---------- Auxiliares ----------

    private PacienteResponse aRespuesta(Paciente p) {
        long segundos = Duration.between(p.fechaEstadoActual(), OffsetDateTime.now(ZoneOffset.UTC))
                .getSeconds();

        return new PacienteResponse(
                p.id(), p.nombre(), p.edad(), p.frecuenciaCardiaca(),
                p.presionSistolica(), p.presionDiastolica(), p.spo2(),
                p.temperatura(), p.dolor(), p.gcs(), p.quejaPrincipal(),
                p.nivel(), p.slaMinutos(), p.estado(), p.escalado(),
                p.fechaRegistro(), p.fechaEscalado(), p.fechaEstadoActual(),
                segundos);
    }

    private int slaPara(NivelTriage nivel) {
        return switch (nivel) {
            case CRITICO -> slaCritico;
            case URGENTE -> slaUrgente;
            case URGENTE_2 -> slaUrgente2;
            case NO_URGENTE -> slaNoUrgente;
            case NO_URGENTE_2 -> slaNoUrgente2;
        };
    }
}