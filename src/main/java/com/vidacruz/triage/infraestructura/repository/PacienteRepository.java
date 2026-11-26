package com.vidacruz.triage.infraestructura.repository;

import com.vidacruz.triage.infraestructura.entity.Paciente;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PacienteRepository extends ReactiveCrudRepository<Paciente, UUID> {

    @Query("""
            SELECT * FROM pacientes
            WHERE estado = 'REGISTRADO'
              AND escalado = FALSE
              AND fecha_registro + (sla_minutos * INTERVAL '1 minute') < now()
            ORDER BY fecha_registro
            """)
    Flux<Paciente> buscarVencidos();


    @Modifying
    @Query("""
            UPDATE pacientes
            SET escalado = TRUE, fecha_escalado = now()
            WHERE id = :id AND estado = 'REGISTRADO' AND escalado = FALSE
            """)
    Mono<Integer> marcarEscalado(@Param("id") UUID id);
}