package com.vidacruz.triage.domain;

import com.vidacruz.triage.domain.enums.NivelTriage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClasificadorTest {
    private final Clasificador clasificador = new Clasificador();

    // clasificar(fc, sistolica, spo2, temperatura, dolor, gcs, queja)

    @Test
    void e1_debeSerCritico() {
        NivelTriage nivel = clasificador.clasificar(130, 180, 97, 36.8, 8, 15, "Dolor torácico");
        assertEquals(NivelTriage.CRITICO, nivel);
    }

    @Test
    void e2_debeSerUrgente() {
        NivelTriage nivel = clasificador.clasificar(95, 150, 91, 37.0, 4, 15, "Dificultad para respirar");
        assertEquals(NivelTriage.URGENTE, nivel);
    }

    @Test
    void e3_debeSerUrgente2() {
        NivelTriage nivel = clasificador.clasificar(88, 130, 98, 38.2, 6, 15, "Fiebre");
        assertEquals(NivelTriage.URGENTE_2, nivel);
    }

    @Test
    void e4_debeSerNoUrgente() {
        NivelTriage nivel = clasificador.clasificar(72, 120, 99, 36.5, 2, 15, "Herida por cortar");
        assertEquals(NivelTriage.NO_URGENTE, nivel);
    }

    @Test
    void e5_debeSerNoUrgente2() {
        NivelTriage nivel = clasificador.clasificar(70, 115, 99, 36.4, 0, 15, "Renovación de recetario");
        assertEquals(NivelTriage.NO_URGENTE_2, nivel);
    }

    @Test
    void e6_debeSerCritico() {
        NivelTriage nivel = clasificador.clasificar(35, 80, 85, 35.9, 0, 6, "Inconsciente");
        assertEquals(NivelTriage.CRITICO, nivel);
    }
}
