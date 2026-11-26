package com.vidacruz.triage.domain;

import com.vidacruz.triage.domain.enums.EstadoPaciente;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaquinaEstadosTest {

    private final MaquinaEstados maquina = new MaquinaEstados();

    // ---- Transiciones válidas ----

    @Test
    void registrado_puedePasarAEnConsulta() {
        assertTrue(maquina.esValida(EstadoPaciente.REGISTRADO, EstadoPaciente.EN_CONSULTA));
    }

    @Test
    void registrado_puedePasarAAltaDirecta() {
        assertTrue(maquina.esValida(EstadoPaciente.REGISTRADO, EstadoPaciente.ALTA));
    }

    @Test
    void enConsulta_puedePasarAAlta() {
        assertTrue(maquina.esValida(EstadoPaciente.EN_CONSULTA, EstadoPaciente.ALTA));
    }

    // ---- Transiciones inválidas ----

    @Test
    void alta_noPuedeVolverAEnConsulta() {
        assertFalse(maquina.esValida(EstadoPaciente.ALTA, EstadoPaciente.EN_CONSULTA));
    }

    @Test
    void alta_noPuedeVolverARegistrado() {
        assertFalse(maquina.esValida(EstadoPaciente.ALTA, EstadoPaciente.REGISTRADO));
    }

    @Test
    void enConsulta_noPuedeVolverARegistrado() {
        assertFalse(maquina.esValida(EstadoPaciente.EN_CONSULTA, EstadoPaciente.REGISTRADO));
    }

    @Test
    void registrado_noPuedeQuedarseEnRegistrado() {
        assertFalse(maquina.esValida(EstadoPaciente.REGISTRADO, EstadoPaciente.REGISTRADO));
    }
}
