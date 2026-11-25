CREATE TABLE IF NOT EXISTS pacientes (
                                         id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre               VARCHAR(100)     NOT NULL,
    edad                 INT              NOT NULL,
    frecuencia_cardiaca  INT              NOT NULL,
    presion_sistolica    INT              NOT NULL,
    presion_diastolica   INT              NOT NULL,
    spo2                 INT              NOT NULL,
    temperatura          DOUBLE PRECISION NOT NULL,
    dolor                INT              NOT NULL,
    gcs                  INT              NOT NULL,
    queja_principal      VARCHAR(500)     NOT NULL,
    nivel                SMALLINT         NOT NULL CHECK (nivel BETWEEN 1 AND 5),
    sla_minutos          INT              NOT NULL CHECK (sla_minutos > 0),
    estado               VARCHAR(20)      NOT NULL CHECK (estado IN ('REGISTRADO', 'EN_CONSULTA', 'ALTA')),
    escalado             BOOLEAN          NOT NULL DEFAULT FALSE,
    fecha_registro       TIMESTAMPTZ      NOT NULL,
    fecha_escalado       TIMESTAMPTZ,
    fecha_estado_actual  TIMESTAMPTZ      NOT NULL
    );

CREATE TABLE IF NOT EXISTS auditoria (
                                         id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         paciente_id      UUID        NOT NULL REFERENCES pacientes (id),
    tipo_evento      VARCHAR(30) NOT NULL
    CHECK (tipo_evento IN ('PACIENTE_REGISTRADO', 'PACIENTE_EN_CONSULTA', 'PACIENTE_ESCALADO', 'PACIENTE_ALTA')),
    estado_anterior  VARCHAR(20),
    estado_nuevo     VARCHAR(20) NOT NULL,
    fecha_evento     TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_pacientes_estado         ON pacientes (estado);
CREATE INDEX IF NOT EXISTS idx_pacientes_fecha_registro ON pacientes (fecha_registro);
-- Índice parcial: el scheduler solo mira pacientes activos sin escalar
CREATE INDEX IF NOT EXISTS idx_pacientes_por_escalar    ON pacientes (fecha_registro)
    WHERE estado = 'REGISTRADO' AND escalado = FALSE;
CREATE INDEX IF NOT EXISTS idx_auditoria_paciente       ON auditoria (paciente_id, fecha_evento);