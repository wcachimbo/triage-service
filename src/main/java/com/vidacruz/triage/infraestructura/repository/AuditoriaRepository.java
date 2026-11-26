package com.vidacruz.triage.infraestructura.repository;


import com.vidacruz.triage.infraestructura.entity.Auditoria;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AuditoriaRepository extends ReactiveCrudRepository<Auditoria, Long> {
}