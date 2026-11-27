package com.vidacruz.triage.domain.exceptions;

public class ReporteInvalidoException extends RuntimeException {

    public ReporteInvalidoException(String mensaje) {
        super(mensaje);
    }
}
