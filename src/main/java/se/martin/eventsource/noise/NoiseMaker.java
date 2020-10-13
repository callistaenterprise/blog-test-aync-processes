package se.martin.eventsource.noise;

import brave.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.martin.eventsource.publish.EventPublisher;

import java.util.UUID;

@Slf4j
@Component
public class NoiseMaker {

    @Autowired
    Tracer tracer;

    @Autowired
    EventPublisher publisher;

    @Scheduled(fixedRate = 3000l)
    public void makeSomeNoise() {
        log.debug("Making some noise...");
        tracer.newTrace();
        publisher.publish(UUID.randomUUID(), -1);
    }
}
