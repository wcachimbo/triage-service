package com.vidacruz.triage.domain.exceptions;

import java.util.UUID;

public class PacienteNoEncontradoException extends RuntimeException {

    public PacienteNoEncontradoException(UUID id) {
        super("No existe un paciente con id " + id);
    }
}