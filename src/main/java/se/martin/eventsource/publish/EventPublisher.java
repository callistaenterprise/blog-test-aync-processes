package se.martin.eventsource.publish;

import brave.Tracer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class EventPublisher {

    private static final String TOPIC = "eventsource";

    @Autowired
    KafkaTemplate template;

    @Autowired
    Tracer tracer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Async
    public void publish(UUID transcationId, Integer sequenceId) {
        var currentThread = Thread.currentThread();
        log.info(
                String.format("%s: Publishing transaction %s and sequence %s",
                        currentThread.getName(),
                        transcationId,
                        sequenceId)
        );

        var metadata = new Metadata(tracer.currentSpan().context().traceIdString());
        char[] padding = new char[1024 * 10];
        Arrays.fill(padding, 'a');
        var event = new Event(metadata, transcationId, sequenceId, new String(padding));
        try {
            String message = objectMapper.writeValueAsString(event);
            String key = UUID.randomUUID().toString();
            Integer partition = random.nextInt(10);

            log.debug(
                    String.format("Publishing event with key %s on topic %s parition %s",
                            key,
                            TOPIC,
                            partition)
            );
            
            var record = new ProducerRecord<>(TOPIC, partition, key, message);
            ListenableFuture<SendResult> future = template.send(record);
            SendResult result = future.get(10, TimeUnit.SECONDS);

            log.debug(String.format("Message published to topic %s partition %s with offset %s",
                    TOPIC,
                    partition,
                    result.getRecordMetadata().offset())
            );
        } catch (JsonProcessingException e) {
            String msg = String.format("Unable to serialize message %s", event);
            log.warn(msg, e);
            // Swallow the exception
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            String msg = String.format("Unable to publish record %s", event);
            log.warn(msg, e);
            // Swallow the exception
        }
    }
}
