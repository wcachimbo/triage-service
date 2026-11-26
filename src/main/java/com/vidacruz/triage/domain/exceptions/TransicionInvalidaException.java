package com.vidacruz.triage.domain.exceptions;

import com.vidacruz.triage.domain.enums.EstadoPaciente;

public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(EstadoPaciente actual, EstadoPaciente nuevo) {
        super("Transición inválida: un paciente en " + actual + " no puede pasar a " + nuevo);
    }
}