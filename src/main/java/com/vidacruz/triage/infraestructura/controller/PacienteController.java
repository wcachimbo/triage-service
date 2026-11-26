package com.vidacruz.triage.infraestructura.controller;

import com.vidacruz.triage.application.PacienteService;
import com.vidacruz.triage.infraestructura.dto.RegistroRequest;
import com.vidacruz.triage.infraestructura.dto.RegistroResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/triage/pacientes")
public class PacienteController {

    private final PacienteService servicio;

    public PacienteController(PacienteService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegistroResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return servicio.registrar(request);
    }
}