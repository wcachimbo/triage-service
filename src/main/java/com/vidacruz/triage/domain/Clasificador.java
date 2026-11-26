package com.vidacruz.triage.domain;

import com.vidacruz.triage.domain.enums.NivelTriage;

import java.util.List;

public class Clasificador {

    private static final List<String> QUEJAS_ADMINISTRATIVAS = List.of(
            "documentos",
            "recolección de resultados",
            "renovación de recetario",
            "vacunación",
            "seguimiento");

    public NivelTriage clasificar(int fc, int sistolica, int spo2,
                                  double temperatura, int dolor, int gcs,
                                  String quejaPrincipal) {

        // Nivel 1: cualquiera de estas condiciones
        if (fc < 40 || fc > 120 || spo2 < 90 || sistolica < 90 || gcs <= 8) {
            return NivelTriage.CRITICO;
        }

        // Nivel 2
        if ((spo2 >= 90 && spo2 <= 93)
                || (sistolica >= 90 && sistolica <= 100)
                || temperatura >= 38.5
                || (gcs >= 9 && gcs <= 13)) {
            return NivelTriage.URGENTE;
        }

        // Nivel 3
        if ((fc >= 90 && fc <= 120)
                || (temperatura >= 38.0 && temperatura < 38.5)
                || dolor >= 7) {
            return NivelTriage.URGENTE_2;
        }

        // Llegó aquí: no cumple nada de los niveles 1 a 3.
        // Es nivel 4, salvo que la queja sea administrativa (nivel 5).
        if (QUEJAS_ADMINISTRATIVAS.contains(quejaPrincipal.toLowerCase().trim())) {
            return NivelTriage.NO_URGENTE_2;
        }
        return NivelTriage.NO_URGENTE;
    }
}