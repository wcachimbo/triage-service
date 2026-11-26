package com.vidacruz.triage.infraestructura.controller;

import com.vidacruz.triage.domain.exceptions.PacienteNoEncontradoException;
import com.vidacruz.triage.domain.exceptions.TransicionInvalidaException;
import com.vidacruz.triage.infraestructura.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;

@RestControllerAdvice
public class ManejadorErrores {

    // 400: algún campo no cumple las validaciones (rango, obligatorio, etc.)
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse datosInvalidos(WebExchangeBindException ex) {
        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .toList();
        return new ErrorResponse(400, "Datos inválidos", detalles);
    }

    // 400: JSON mal formado, un texto donde va un número, un id que no es UUID...
    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse peticionMalFormada(ServerWebInputException ex) {
        return new ErrorResponse(400, "Petición mal formada",
                List.of("El cuerpo o los parámetros de la petición no tienen un formato válido"));
    }

    // 404: el paciente no existe
    @ExceptionHandler(PacienteNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse noEncontrado(PacienteNoEncontradoException ex) {
        return new ErrorResponse(404, "No encontrado", List.of(ex.getMessage()));
    }

    // 409: el cambio de estado no está permitido
    @ExceptionHandler(TransicionInvalidaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse transicionInvalida(TransicionInvalidaException ex) {
        return new ErrorResponse(409, "Conflicto", List.of(ex.getMessage()));
    }
}
