package com.vidacruz.triage.infraestructura.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record ReporteHoraResponse(
        OffsetDateTime hora,
        long totalRegistrados,
        Map<String, Long> porNivel,
        BigDecimal esperaPromedioMinutos,
        BigDecimal esperaP95Minutos) {
}