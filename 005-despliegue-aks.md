# ADR 005: Despliegue en AKS con la infraestructura dentro del cluster

## Contexto
El enunciado pide manifests de Kubernetes con Azure como objetivo (AKS o Azure Container Apps) y una URL pública con las APIs accesibles. La suscripción disponible es de tipo estudiante, con restricciones de regiones y de tamaños de máquina.

## Decisión
- **AKS**, porque los manifests de Kubernetes que se entregan son exactamente los que se despliegan (Azure Container Apps habría requerido otro formato de configuración).
- Imágenes construidas con **Azure Container Registry**, a partir de los Dockerfiles de cada servicio (compilación en dos etapas, usuario sin privilegios).
- **PostgreSQL, Redis y Kafka corren como pods dentro del cluster**, en lugar de servicios administrados (Azure Database for PostgreSQL, Azure Cache for Redis, Event Hubs).
- La región y el tamaño de nodo se eligieron entre los que permite la política de la suscripción (región `westus`, un nodo `Standard_D2s_v6`).
- Los **secretos no están en el repositorio**: se crean en el cluster con `kubectl create secret`. Los manifests solo los referencian.
- Cada servicio expone su API con un `Service` de tipo `LoadBalancer`.
- Probes de startup, liveness y readiness sobre los endpoints de Actuator, y **límites de memoria** en todos los pods para que quepan en un solo nodo.
- `enableServiceLinks: false`: sin esto, Kubernetes inyecta variables de entorno (por ejemplo `KAFKA_PORT`, por el Service llamado `kafka`) que Kafka interpreta como configuración propia y no arranca.

## Justificación
- Un solo despliegue declarativo, barato y rápido de crear y de destruir, suficiente para la evaluación.
- Los nombres de los Services coinciden con los del `docker-compose`, así que los servicios encuentran sus dependencias por nombre en ambos entornos.

## Consecuencias
- **Los datos no son duraderos** (almacenamiento efímero): si un pod de PostgreSQL, Kafka o Redis se reinicia, se pierden. En producción se usarían servicios administrados o volúmenes persistentes, con réplicas.
- **Un solo nodo y una réplica por servicio:** no hay alta disponibilidad.
- **HTTP sin cifrar:** la API key viaja en claro. La mejora es un Ingress con TLS.
- Cualquiera que tenga la API key puede consumir el asistente y gastar la cuota del proveedor de IA: conviene fijar un límite de gasto en el proveedor.
- El cluster genera costo mientras exista: se elimina con `az group delete` al terminar la evaluación.
