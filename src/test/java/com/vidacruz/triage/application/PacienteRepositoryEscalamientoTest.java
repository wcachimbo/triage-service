package com.vidacruz.triage.application;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import com.vidacruz.triage.domain.enums.NivelTriage;
import com.vidacruz.triage.infraestructura.entity.Paciente;
import com.vidacruz.triage.infraestructura.repository.PacienteRepository;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataR2dbcTest
@Testcontainers
class PacienteRepositoryEscalamientoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PacienteRepository repositorio;

    @Autowired
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void prepararBaseLimpia() {
        // Crea las tablas con el mismo schema.sql de la aplicación y vacía los pacientes
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"))
                .populate(connectionFactory)
                .block();
        repositorio.deleteAll().block();
    }

    private Mono<Paciente> guardar(EstadoPaciente estado, long minutosDesdeRegistro) {
        OffsetDateTime registro = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(minutosDesdeRegistro);
        return repositorio.save(new Paciente(null, "Paciente de prueba", 40, 80, 120, 80, 98, 36.5,
                0, 15, "Dolor de cabeza", NivelTriage.URGENTE, 10, estado,
                false, registro, null, registro));
    }

    @Test
    void pacienteConSlaVencido_apareceComoPorEscalar() {
        Paciente p = guardar(EstadoPaciente.REGISTRADO, 30).block();

        StepVerifier.create(repositorio.buscarVencidos())
                .expectNextMatches(encontrado -> encontrado.id().equals(p.id()))
                .verifyComplete();
    }

    @Test
    void pacienteDentroDeSuSla_noApareceComoPorEscalar() {
        guardar(EstadoPaciente.REGISTRADO, 2).block();   // SLA 10 min, lleva 2

        StepVerifier.create(repositorio.buscarVencidos())
                .verifyComplete();   // vacío
    }

    @Test
    void marcarEscalado_soloFuncionaLaPrimeraVez() {
        Paciente p = guardar(EstadoPaciente.REGISTRADO, 30).block();

        StepVerifier.create(repositorio.marcarEscalado(p.id())).expectNext(1).verifyComplete();
        StepVerifier.create(repositorio.marcarEscalado(p.id())).expectNext(0).verifyComplete();

        Paciente actualizado = repositorio.findById(p.id()).block();
        assertTrue(actualizado.escalado());
        assertNotNull(actualizado.fechaEscalado());
        StepVerifier.create(repositorio.buscarVencidos()).verifyComplete();
    }

    @Test
    void pacienteEnConsulta_nuncaSeEscala() {
        Paciente p = guardar(EstadoPaciente.EN_CONSULTA, 30).block();

        StepVerifier.create(repositorio.buscarVencidos()).verifyComplete();
        StepVerifier.create(repositorio.marcarEscalado(p.id())).expectNext(0).verifyComplete();
        assertFalse(repositorio.findById(p.id()).block().escalado());
    }

    @Test
    void pacienteConAlta_nuncaSeEscala() {
        Paciente p = guardar(EstadoPaciente.ALTA, 30).block();

        StepVerifier.create(repositorio.buscarVencidos()).verifyComplete();
        StepVerifier.create(repositorio.marcarEscalado(p.id())).expectNext(0).verifyComplete();
        assertFalse(repositorio.findById(p.id()).block().escalado());
    }

    @Test
    void dosEscalamientosSimultaneos_soloUnoGana() {
        Paciente p = guardar(EstadoPaciente.REGISTRADO, 30).block();

        StepVerifier.create(
                        Mono.zip(repositorio.marcarEscalado(p.id()), repositorio.marcarEscalado(p.id()))
                                .map(par -> par.getT1() + par.getT2()))
                .expectNext(1)
                .verifyComplete();
    }
}
