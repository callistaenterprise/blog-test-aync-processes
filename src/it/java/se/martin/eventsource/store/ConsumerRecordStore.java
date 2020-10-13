package se.martin.eventsource.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Slf4j
class ConsumerRecordStore implements Runnable {

    private final static String DEFAULT_BROKER = "localhost:19092";
    private final static String TOPIC = "eventsource";
    private final static String CONSUMER_GROUP_ID = "eventsource_integrationtest_" + System.currentTimeMillis();

    private final static Duration MAX_POLLING_TIMEOUT = Duration.of(1, ChronoUnit.SECONDS);

    private final Consumer<String, String> consumer;

    private Map<String, ExpirableString> store;

    private long nextCleanup;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    ConsumerRecordStore() {

        String broker = Optional.ofNullable(System.getProperty("kafka.host"))
                .orElse(DEFAULT_BROKER);
        log.info("Connecting to broker on host " + broker);

        log.info("Initialising kafka consumer on thread " + Thread.currentThread().getName());

        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Set up the store
        store = new HashMap<>();

        // Create the consumer using props.
        consumer = new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));

        // Initialise the cleanup time
        nextCleanup = System.currentTimeMillis() + 30000l;
    }

    @Override
    public void run() {
        while (true) {
            log.debug("Polling topic " + TOPIC);

            var records = consumer.poll(MAX_POLLING_TIMEOUT);
            consumer.commitAsync();

            lock.writeLock().lock();

            records.forEach(r -> {
                log.debug(
                        String.format("Storing record with key %s from partition %s offset %s",
                                r.key(),
                                r.partition(),
                                r.offset()));
                store.put(r.key(), new ExpirableString(r.value()));
            });

            lock.writeLock().unlock();

            cleanUp();
        }
    }

    List<String> getRecords() {
        lock.readLock().lock();

        var records = store.values().stream()
                .map(e -> e.value)
                .collect(Collectors.toList());

        lock.readLock().unlock();

        return records;
    }

    private static class ExpirableString {

        private final String value;
        private final long expireTime;

        private ExpirableString(String value) {
            expireTime = System.currentTimeMillis() + 30000l;
            this.value = value;
        }
    }

    private void cleanUp() {
        final long now = System.currentTimeMillis();
        if (now <= nextCleanup) {
            return;
        }

        lock.writeLock().lock();

        log.debug("Cleaning up...");
        nextCleanup = now + 30000l;
        var oldSize = store.size();
        var newStore = store.entrySet().stream()
                .filter(e -> e.getValue().expireTime > now)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        store = newStore;
        var newSize = store.size();
        log.debug("Reduced map from " + oldSize + " to " + newSize);
        lock.writeLock().unlock();
    }

}
