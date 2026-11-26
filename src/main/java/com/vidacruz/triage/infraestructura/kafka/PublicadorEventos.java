package com.vidacruz.triage.infraestructura.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
public class PublicadorEventos {
    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final KafkaSender<String, String> sender;
    private final ObjectMapper mapper;
    private final String topic;

    public PublicadorEventos(KafkaSender<String, String> sender,
                             ObjectMapper mapper,
                             @Value("${triage.kafka.topic}") String topic) {
        this.sender = sender;
        this.mapper = mapper;
        this.topic = topic;
    }

    public Mono<Void> publicar(EventoPaciente evento) {
        String json;
        try {
            json = mapper.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }


        var registro = new ProducerRecord<>(topic, evento.pacienteId().toString(), json);

        return sender.send(Mono.just(SenderRecord.create(registro, evento.pacienteId())))
                .doOnNext(r -> log.info("Evento {} publicado para el paciente {}",
                        evento.tipo(), evento.pacienteId()))
                .then();
    }
}