package com.vidacruz.triage.infraestructura.controller;

import com.vidacruz.triage.application.ReporteService;
import com.vidacruz.triage.infraestructura.dto.ReporteHoraResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/triage/reportes")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/operacion")
    public Flux<ReporteHoraResponse> operacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return servicio.operacion(desde, hasta);
    }
}