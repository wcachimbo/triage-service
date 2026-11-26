package com.vidacruz.triage.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "nombre es obligatorio")
        @Size(min = 3, max = 100, message = "nombre debe tener entre 3 y 100 caracteres")
        String nombre,

        @NotNull(message = "edad es obligatoria")
        @Min(value = 0, message = "edad debe estar entre 0 y 120")
        @Max(value = 120, message = "edad debe estar entre 0 y 120")
        Integer edad,

        @NotNull(message = "frecuenciaCardiaca es obligatoria")
        @Min(value = 20, message = "frecuenciaCardiaca debe estar entre 20 y 250")
        @Max(value = 250, message = "frecuenciaCardiaca debe estar entre 20 y 250")
        Integer frecuenciaCardiaca,

        @NotNull(message = "presionSistolica es obligatoria")
        @Min(value = 60, message = "presionSistolica debe estar entre 60 y 260")
        @Max(value = 260, message = "presionSistolica debe estar entre 60 y 260")
        Integer presionSistolica,

        @NotNull(message = "presionDiastolica es obligatoria")
        @Min(value = 30, message = "presionDiastolica debe estar entre 30 y 160")
        @Max(value = 160, message = "presionDiastolica debe estar entre 30 y 160")
        Integer presionDiastolica,

        @NotNull(message = "spo2 es obligatorio")
        @Min(value = 70, message = "spo2 debe estar entre 70 y 100")
        @Max(value = 100, message = "spo2 debe estar entre 70 y 100")
        Integer spo2,

        @NotNull(message = "temperatura es obligatoria")
        @DecimalMin(value = "34.0", message = "temperatura debe estar entre 34.0 y 42.0")
        @DecimalMax(value = "42.0", message = "temperatura debe estar entre 34.0 y 42.0")
        Double temperatura,

        @NotNull(message = "dolor es obligatorio")
        @Min(value = 0, message = "dolor debe estar entre 0 y 10")
        @Max(value = 10, message = "dolor debe estar entre 0 y 10")
        Integer dolor,

        @NotNull(message = "gcs es obligatorio")
        @Min(value = 3, message = "gcs debe estar entre 3 y 15")
        @Max(value = 15, message = "gcs debe estar entre 3 y 15")
        Integer gcs,

        @NotBlank(message = "quejaPrincipal es obligatoria")
        String quejaPrincipal) {

    // Regla extra de la prueba: la diastólica siempre debe ser menor que la sistólica
    @JsonIgnore
    @AssertTrue(message = "presionDiastolica debe ser menor que presionSistolica")
    public boolean isDiastolicaMenorQueSistolica() {
        if (presionSistolica == null || presionDiastolica == null) {
            return true; // ya lo avisa @NotNull
        }
        return presionDiastolica < presionSistolica;
    }
}
