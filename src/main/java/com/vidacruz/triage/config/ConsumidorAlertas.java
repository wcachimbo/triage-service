package com.vidacruz.triage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacruz.triage.application.Notificador;
import com.vidacruz.triage.domain.AlertaEscalamiento;
import com.vidacruz.triage.infraestructura.kafka.EventoPaciente;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConsumidorAlertas {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorAlertas.class);

    private final Notificador notificador;
    private final ObjectMapper mapper;
    private final String servidores;
    private final String topic;
    private final String groupId;
    private final boolean habilitado;

    private Disposable suscripcion;

    public ConsumidorAlertas(Notificador notificador,
                             ObjectMapper mapper,
                             @Value("${triage.kafka.bootstrap-servers}") String servidores,
                             @Value("${triage.kafka.topic}") String topic,
                             @Value("${triage.alertas.group-id}") String groupId,
                             @Value("${triage.alertas.habilitado}") boolean habilitado) {
        this.notificador = notificador;
        this.mapper = mapper;
        this.servidores = servidores;
        this.topic = topic;
        this.groupId = groupId;
        this.habilitado = habilitado;
    }

    // Se arranca cuando la aplicación ya está lista
    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (!habilitado) {
            log.info("Consumidor de alertas deshabilitado");
            return;
        }

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servidores);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        ReceiverOptions<String, String> opciones = ReceiverOptions.<String, String>create(props)
                .subscription(List.of(topic));

        suscripcion = KafkaReceiver.create(opciones)
                .receive()
                .concatMap(this::procesar)
                // Si Kafka se cae, reintenta conectar sin matar la aplicación
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30)))
                .subscribe();

        log.info("Consumidor de alertas escuchando el topic {}", topic);
    }

    private Mono<Void> procesar(ReceiverRecord<String, String> registro) {
        return Mono.fromCallable(() -> mapper.readValue(registro.value(), EventoPaciente.class))
                // Solo nos interesan los escalamientos; el resto se ignora
                .filter(evento -> "paciente_escalado".equals(evento.tipo()))
                .map(evento -> new AlertaEscalamiento(evento.pacienteId(), evento.nivel(), evento.timestamp()))
                .flatMap(notificador::alertar)
                .onErrorResume(error -> {
                    log.error("No se pudo procesar el mensaje de Kafka", error);
                    return Mono.empty();
                })
                // Marcamos el mensaje como leído al terminar
                .doFinally(senal -> registro.receiverOffset().acknowledge());
    }

    @PreDestroy
    public void detener() {
        if (suscripcion != null) {
            suscripcion.dispose();
        }
    }
}