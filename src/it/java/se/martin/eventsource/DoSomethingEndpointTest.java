package se.martin.eventsource;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import se.martin.eventsource.availability.CheckAvailabilityExtension;
import se.martin.eventsource.store.EventStore;
import se.martin.eventsource.store.EventStoreResolver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

@Slf4j
@ExtendWith({CheckAvailabilityExtension.class, EventStoreResolver.class})
@Execution(CONCURRENT)
public class DoSomethingEndpointTest {

    private static final String DEFAULT_HOST = "localhost:8097";

    private static final String TRACE_ID_HEADER = "x-b3-traceid";

    private final static HttpClient CLIENT = HttpClient.newHttpClient();

    private static URI uri;

    private final EventStore eventStore;

    @BeforeAll
    public static void init() {
        var host = Optional.ofNullable(System.getProperty("eventsource.host")).orElse(DEFAULT_HOST);
        var endpoint = "http://" + host + "/dosomething";
        log.info("Using endpoint at URI " + endpoint);
        uri = URI.create(endpoint);
    }

    public DoSomethingEndpointTest(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    // This test initiates an HttpRequest to the doSomething endpoint and tests whether five events are published
    // in sequential order.
    // This test fails as soon as events are published to multiple partitions and will not work if tests are
    // run in parallel.
    // This test is intentionally disabled. Enabling it will result in a failing test.
    @Test
    @Disabled
    public void testForSequentialIntegrity() throws IOException, InterruptedException, TimeoutException, ExecutionException {

        log.info("Running on... " + Thread.currentThread().getName());

        // Given: a request is to be sent to the "do something" endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        // When the request is sent
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // Then the request is successful
        assertEquals(200, response.statusCode());

        // And 5 events have been published
        var events = eventStore.getEvents("astringsothatthiscompiles");
        assertEquals(5, events.size());

        // Ensure that the sequence of the exents is correct
        var sequenceIds = events.stream()
                .map(json -> JsonPath.parse(json).read("$['sequenceId']", Integer.class))
                .collect(Collectors.toList());
        assertEquals(Integer.valueOf(1), sequenceIds.get(0));
        assertEquals(Integer.valueOf(2), sequenceIds.get(1));
        assertEquals(Integer.valueOf(3), sequenceIds.get(2));
        assertEquals(Integer.valueOf(4), sequenceIds.get(3));
        assertEquals(Integer.valueOf(5), sequenceIds.get(4));
    }

    // This test initiates an HttpRequest to the doSomething endpoint and tests that exactly five events are published
    // and that an event with each unique sequence id is published.
    @RepeatedTest(value = 100, name = RepeatedTest.LONG_DISPLAY_NAME)
    public void testThatAllEventsArePublished() throws IOException, InterruptedException, TimeoutException, ExecutionException {

        log.info("Running on... " + Thread.currentThread().getName());

        // Given: a request is to be sent to the "do something" endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        // When the request is sent
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Response: " + response.toString());

        // Then the request is successful
        assertEquals(200, response.statusCode());

        // And the response contains a trace id
        var traceIdHeader = response.headers().firstValue(TRACE_ID_HEADER);
        assertTrue(traceIdHeader.isPresent());
        var traceId = traceIdHeader.get();

        // And 5 events have been published
        var events = eventStore.getEvents(traceId);
        assertEquals(5, events.size());

        // And the events had sequence ids from 1 to 5 (inclusive)
        var sequenceIds = events.stream()
                .map(json -> JsonPath.parse(json).read("$['sequenceId']", Integer.class))
                .collect(Collectors.toSet());
        IntStream.range(1, 6).forEach(i -> assertTrue(sequenceIds.contains(i)));
    }

}
