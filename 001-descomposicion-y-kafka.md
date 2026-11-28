# ADR 001: Dos servicios e integración asíncrona por Kafka

## Contexto
El enunciado pide como mínimo dos microservicios en Java reactivo (Triage y Asistente Clínico), Kafka como integración y que el servicio de triaje nunca dependa de la disponibilidad del asistente. Cualquier descomposición adicional debe justificarse.

## Decisión
- Exactamente **dos servicios**: `triage-service` (registro, clasificación, estados, SLA, auditoría, reporte y alertas) y `asistente-service` (RAG y vista de urgencias). No se hizo una descomposición adicional: con el alcance dado, más servicios sumarían despliegues y latencia de red sin una frontera de negocio que lo justifique.
- Un único topic, `triaje.eventos`, con cuatro tipos de evento. Mensajes en **JSON**, con la **key igual al id del paciente**, para que los eventos de un mismo paciente lleguen en orden (Kafka solo garantiza orden dentro de una partición).
- Todos los eventos incluyen el `nivel`, de modo que el Asistente reconstruye el estado sin consultar a triage.
- Cada consumidor usa su propio *consumer group*: `triage-alertas` (lee desde "ahora") y `asistente-estado-urgencias` (lee desde el principio, para poder reconstruir la vista).
- Cada servicio mantiene su **propia copia** de la clase del evento. Solo comparten el formato JSON; así no quedan acoplados por una librería común.

## Justificación
- Triage solo publica; no sabe si alguien consume. Si el Asistente cae, la atención no se ve afectada.
- El Asistente reconstruye su vista con eventos, como exige el enunciado (no llama a la API de triage).
- JSON en lugar de Avro: es legible, simple de depurar y suficiente para el alcance. Avro con un registro de esquemas aportaría validación de compatibilidad, a costa de otra pieza de infraestructura.
- El Asistente guarda en Redis un hash `id -> nivel`. Se agrega al registrar y se borra en el alta. Es idempotente: reprocesar un mensaje no altera la cuenta (con contadores, un mensaje repetido la dañaría).

## Consecuencias
- Consistencia eventual: la vista del Asistente puede ir segundos por detrás de triage.
- Entrega "al menos una vez": los consumidores deben tolerar mensajes repetidos (lo hacen por diseño).
- El evento se publica dentro de la transacción de triage, sin patrón Outbox. Es consistente en el caso normal, pero hace que triage dependa de Kafka para escribir y deja una ventana pequeña de inconsistencia. El Outbox (guardar el evento en una tabla en la misma transacción y publicarlo en segundo plano) queda como mejora.
- La reconstrucción de la vista está limitada por la retención del topic.
