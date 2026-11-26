package com.vidacruz.triage.infraestructura.repository;

import com.vidacruz.triage.infraestructura.entity.Paciente;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface PacienteRepository extends ReactiveCrudRepository<Paciente, UUID> {
}