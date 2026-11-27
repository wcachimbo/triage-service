package com.vidacruz.triage.infraestructura.repository;

import com.vidacruz.triage.infraestructura.dto.ReporteHoraResponse;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class ReporteRepository {

    private static final String SQL_OPERACION = """
            WITH primera_consulta AS (
                -- Numera las entradas a consulta de cada paciente por fecha (1 = la primera)
                SELECT paciente_id,
                       fecha_evento,
                       ROW_NUMBER() OVER (PARTITION BY paciente_id ORDER BY fecha_evento) AS orden
                FROM auditoria
                WHERE tipo_evento = 'PACIENTE_EN_CONSULTA'
            ),
            base AS (
                -- Un paciente por fila: su hora de registro, nivel y minutos de espera
                SELECT p.nivel,
                       date_trunc('hour', p.fecha_registro, 'UTC') AS hora,
                       EXTRACT(EPOCH FROM (pc.fecha_evento - p.fecha_registro)) / 60.0 AS espera_min
                FROM pacientes p
                LEFT JOIN primera_consulta pc
                       ON pc.paciente_id = p.id AND pc.orden = 1
                WHERE p.fecha_registro >= :desde
                  AND p.fecha_registro <  :hasta
            )
            SELECT hora,
                   COUNT(*)                                       AS total,
                   COUNT(*) FILTER (WHERE nivel = 'CRITICO')      AS critico,
                   COUNT(*) FILTER (WHERE nivel = 'URGENTE')      AS urgente,
                   COUNT(*) FILTER (WHERE nivel = 'URGENTE_2')    AS urgente_2,
                   COUNT(*) FILTER (WHERE nivel = 'NO_URGENTE')   AS no_urgente,
                   COUNT(*) FILTER (WHERE nivel = 'NO_URGENTE_2') AS no_urgente_2,
                   ROUND(CAST(AVG(espera_min) AS numeric), 2)     AS espera_promedio_min,
                   ROUND(CAST(percentile_cont(0.95) WITHIN GROUP (ORDER BY espera_min) AS numeric), 2)
                                                                  AS espera_p95_min
            FROM base
            GROUP BY hora
            ORDER BY hora
            """;

    private final DatabaseClient client;

    public ReporteRepository(DatabaseClient client) {
        this.client = client;
    }

    public Flux<ReporteHoraResponse> operacionPorHora(OffsetDateTime desde, OffsetDateTime hasta) {
        return client.sql(SQL_OPERACION)
                .bind("desde", desde)
                .bind("hasta", hasta)
                .map((row, meta) -> {
                    Map<String, Long> porNivel = new LinkedHashMap<>();
                    porNivel.put("CRITICO", row.get("critico", Long.class));
                    porNivel.put("URGENTE", row.get("urgente", Long.class));
                    porNivel.put("URGENTE_2", row.get("urgente_2", Long.class));
                    porNivel.put("NO_URGENTE", row.get("no_urgente", Long.class));
                    porNivel.put("NO_URGENTE_2", row.get("no_urgente_2", Long.class));

                    return new ReporteHoraResponse(
                            row.get("hora", OffsetDateTime.class),
                            row.get("total", Long.class),
                            porNivel,
                            row.get("espera_promedio_min", BigDecimal.class),
                            row.get("espera_p95_min", BigDecimal.class));
                })
                .all();
    }
}
