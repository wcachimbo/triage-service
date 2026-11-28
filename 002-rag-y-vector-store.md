# ADR 002: RAG con vector store en memoria y umbral de similitud calibrado

## Contexto
El asistente nunca puede inventar contenido clínico: toda respuesta debe sustentarse en el corpus oficial (3 protocolos, mínimo) y citar la fuente. Si no hay protocolo relevante, la respuesta debe ser únicamente el aviso de que no existe protocolo disponible (RN-05). Hay que elegir framework de RAG y vector store.

## Decisión
- **Spring AI 1.1.x** (compatible con Spring Boot 3.5) con **OpenAI**: modelo de embeddings `text-embedding-3-small` y modelo de chat configurable (`gpt-4o-mini` por defecto) con temperatura 0.
- **`SimpleVectorStore` en memoria.** El corpus (3 protocolos) se parte en **fragmentos por sección** (definición, medidas iniciales, contraindicaciones...), cada uno con metadata `protocolo` y `seccion`, y se vectoriza al arrancar.
- **Anti-alucinación en capas:**
  1. *Umbral de similitud:* si ningún fragmento lo supera, se responde el aviso fijo **sin llamar al modelo**.
  2. *Prompt estricto:* responder solo con el contexto recuperado; si no alcanza, devolver la marca `SIN_PROTOCOLO`, que se traduce al aviso fijo.
  3. *Fuentes desde la metadata* de los fragmentos recuperados, no desde el texto del modelo.
  4. *Validación:* cada fuente debe existir en el corpus; una respuesta sin fuente válida se descarta.
- **Umbral = 0.53**, configurable (`asistente.rag.umbral-similitud`).

## Calibración del umbral
Puntaje del mejor fragmento para las consultas de ejemplo del enunciado:

| Consulta | Mejor fragmento | Puntaje | Debe responder |
|---|---|---|---|
| Medidas iniciales en crisis hipertensiva | PROTO-001 / Medidas iniciales | 0.675 | Sí |
| Primer antibiótico en sepsis | PROTO-002 / Acciones iniciales | 0.578 | Sí |
| Tratamiento de diabetes tipo 2 | PROTO-003 / Clasificación | 0.479 | No |

El umbral es el punto medio entre el peor caso válido (0.578) y el mejor caso inválido (0.479). Con 0.45, la pregunta de diabetes habría pasado el filtro y la de crisis hipertensiva habría citado, además, un protocolo de deshidratación sin relación.

## Justificación
- Con 3 protocolos, un vector store externo (pgvector, Redis) añadiría infraestructura sin beneficio.
- Fragmentar por sección da citas precisas (`PROTO-001`, "Medidas iniciales") y mejora la recuperación.
- Spring AI es bloqueante: las llamadas se ejecutan en `Schedulers.boundedElastic()` para no bloquear el event loop de WebFlux.
- El modelo va detrás de la interfaz `ClienteLlm`, que se reemplaza por un mock en los tests.

## Consecuencias
- El índice se pierde al reiniciar y se reconstruye al arrancar, lo que requiere llamar al proveedor de IA. Si esa carga falla, el store queda vacío y el asistente responde "no existe protocolo" hasta reiniciar.
- **El umbral se calibró con una muestra pequeña** (márgenes de unos 0.05). Una pregunta válida redactada distinto podría quedar por debajo, y una inválida de tema cercano por encima; en ese caso quedan las capas 2 a 4. Hay que revalidarlo si el corpus crece.
- Las fuentes citadas son los fragmentos entregados como contexto, no necesariamente cada uno de los que el modelo usó.
- Para escalar el corpus: pgvector o Redis como vector store, y cargar el corpus desde archivos.
