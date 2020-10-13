package se.martin.eventsource.availability;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

@Slf4j
public class CheckAvailabilityExtension implements BeforeAllCallback {

    private static final String DEFAULT_HOST = "localhost:8097";

    private final HttpClient client = HttpClient.newHttpClient();

    private final URI uri;

    private CheckAvailabilityExtension() {
        String host = Optional.ofNullable(System.getProperty("eventsource.host"))
                .orElse(DEFAULT_HOST);

        String healthcheckUri = "http://" + host + "/actuator/health";
        log.info("Querying healthcheck URI " + healthcheckUri);
        uri = URI.create(healthcheckUri);
    }

    @Override
    public void beforeAll(ExtensionContext context) {

        final Instant stopTime = Instant.now().plusSeconds(60);

        boolean isAvailable = false;
        while (!isAvailable && stopTime.isAfter(Instant.now())) {
            try {
                log.debug("Pinging healthcheck endpoint");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .GET()
                        .build();
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.body().contains("UP")) {
                        isAvailable = true;
                    }
                } catch (Exception e) {
                    // do nothing
                }
                // Block
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        if (!isAvailable) {
            throw new PreconditionViolationException("Unable to get healthy indicator from application healthcheck.");
        }
    }

}
