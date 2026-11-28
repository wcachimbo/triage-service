# ADR 004: Modo degradado y circuit breaker sobre el proveedor de IA

## Contexto
El proveedor de IA es una dependencia externa que puede fallar o responder lento. El enunciado exige un circuit breaker (Resilience4j) con un fallback explícito: respuesta 503 con un mensaje claro cuando el LLM no está disponible. El servicio de triaje nunca debe depender de esa disponibilidad.

## Decisión
- Todo el flujo que habla con el proveedor (búsqueda en el vector store, que calcula el embedding de la pregunta, y la llamada al modelo de chat) va dentro de **un solo circuit breaker** (`proveedor-ia`).
- Configuración: ventana de 10 llamadas, mínimo 5 para decidir, abre con 50% o más de fallos, permanece abierto 30 s y en estado semiabierto deja pasar 2 llamadas de prueba.
- **Timeout de 20 s** sobre el flujo completo, aplicado antes del breaker, de modo que una llamada lenta cuente como falla.
- Cualquier error del proveedor (incluido el circuito abierto) se convierte en `LlmNoDisponibleException`, que el manejador global responde como **503** con un mensaje claro.
- Los reintentos internos de Spring AI se acotan a 2 intentos para fallar rápido.

## Justificación
- Con el circuito abierto las respuestas 503 salen al instante, en lugar de hacer esperar a cada médico un timeout.
- Se protege también la búsqueda, porque calcular el embedding de la pregunta usa la API del mismo proveedor.
- El modo degradado se limita al endpoint de consulta. `GET /asistente/estado-urgencias` no usa el proveedor y sigue funcionando; triage no se entera.

## Consecuencias
- Durante la falla, los médicos no tienen asistente. No hay respuesta alternativa generada sin el modelo: es lo correcto en un contexto clínico, donde un contenido improvisado sería peor que ninguno.
- Los umbrales (50%, 30 s, 20 s) son valores razonables, no medidos con tráfico real, y conviene ajustarlos con datos.
- El comportamiento queda cubierto por tests: fallo del proveedor y circuito abierto devuelven `LlmNoDisponibleException`, y en el segundo caso no se llama al modelo.
