package se.martin.eventsource.store;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class EventStore {

    private final static long MAX_WAIT_TIME = 10000l;

    private ConsumerRecordStore store;

    public List<String> getEvents(String traceId) {
        var thread = Thread.currentThread().getName();

        if (store == null) {
            synchronized (this) {
                if (store == null) {
                    log.debug(thread + ": Initialising event store");
                    store = new ConsumerRecordStore();
                    Executors.newSingleThreadExecutor().submit(store);
                }
            }
        }

        try {
            log.debug(thread + ": sleeping");
            Thread.sleep(MAX_WAIT_TIME);
        } catch (InterruptedException e) {
            // do nothing
        }

        log.debug(thread + ": Fetching records");
        List<String> records = store.getRecords();
        log.info("Parsing " + records.size() + " records...");
        var parsedRecords = records.stream()
                .filter(s -> traceId.equals(JsonPath.parse(s).read("$['metadata']['traceId']", String.class)))
                .collect(Collectors.toList());
        log.debug(thread + ": Returning " + parsedRecords.size() + " records");
        return parsedRecords;
    }

}
