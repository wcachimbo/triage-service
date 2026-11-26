package com.vidacruz.triage.infraestructura.dto;


import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        List<String> detalles) {
}