package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.exceptions.ReporteInvalidoException;
import com.vidacruz.triage.infraestructura.dto.ReporteHoraResponse;
import com.vidacruz.triage.infraestructura.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class ReporteService {

    private static final Duration RANGO_MAXIMO = Duration.ofDays(31);

    private final ReporteRepository repositorio;

    public ReporteService(ReporteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Flux<ReporteHoraResponse> operacion(OffsetDateTime desde, OffsetDateTime hasta) {
        if (!desde.isBefore(hasta)) {
            return Flux.error(new ReporteInvalidoException("'desde' debe ser anterior a 'hasta'"));
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            return Flux.error(new ReporteInvalidoException("El rango máximo del reporte es de 31 días"));
        }
        return repositorio.operacionPorHora(desde, hasta);
    }
}