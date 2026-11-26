package com.vidacruz.triage;

import com.vidacruz.triage.domain.enums.EstadoPaciente;

public class MaquinaEstados {

    public boolean esValida(EstadoPaciente actual, EstadoPaciente nuevo) {

        if (actual == EstadoPaciente.REGISTRADO) {
            return nuevo == EstadoPaciente.EN_CONSULTA
                    || nuevo == EstadoPaciente.ALTA;
        }

        if (actual == EstadoPaciente.EN_CONSULTA) {
            return nuevo == EstadoPaciente.ALTA;
        }

        // Si está en ALTA, no puede ir a ningún lado
        return false;
    }
}
