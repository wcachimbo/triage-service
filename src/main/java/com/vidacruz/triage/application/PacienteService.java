package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.Clasificador;
import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;
import com.vidacruz.triage.infraestructura.dto.RegistroRequest;
import com.vidacruz.triage.infraestructura.dto.RegistroResponse;
import com.vidacruz.triage.infraestructura.entity.Paciente;
import com.vidacruz.triage.infraestructura.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class PacienteService {

    private final PacienteRepository repositorio;
    private final Clasificador clasificador = new Clasificador();

    // Los minutos de SLA vienen de application.properties
    private final int slaCritico;
    private final int slaUrgente;
    private final int slaUrgente2;
    private final int slaNoUrgente;
    private final int slaNoUrgente2;

    public PacienteService(PacienteRepository repositorio,
                           @Value("${triage.sla.critico}") int slaCritico,
                           @Value("${triage.sla.urgente}") int slaUrgente,
                           @Value("${triage.sla.urgente-2}") int slaUrgente2,
                           @Value("${triage.sla.no-urgente}") int slaNoUrgente,
                           @Value("${triage.sla.no-urgente-2}") int slaNoUrgente2) {
        this.repositorio = repositorio;
        this.slaCritico = slaCritico;
        this.slaUrgente = slaUrgente;
        this.slaUrgente2 = slaUrgente2;
        this.slaNoUrgente = slaNoUrgente;
        this.slaNoUrgente2 = slaNoUrgente2;
    }

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

        // 4. Guardar y convertir a la respuesta
        return repositorio.save(nuevo)
                .map(p -> new RegistroResponse(
                        p.id(), p.nivel(), p.slaMinutos(), p.estado(), p.fechaRegistro()));
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