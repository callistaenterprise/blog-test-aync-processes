package se.martin.eventsource.dosomething;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import se.martin.eventsource.publish.EventPublisher;

import java.util.UUID;

@RestController
@Slf4j
public class DoSomethingResource {

    @Autowired
    EventPublisher publisher;

    @PostMapping("/dosomething")
    public ResponseEntity<Transaction> doSomething() {
        var transactionId = UUID.randomUUID();

        // Do some state changes...

        // Publish five events
        publisher.publish(transactionId, 1);
        publisher.publish(transactionId, 2);
        publisher.publish(transactionId, 3);
        publisher.publish(transactionId, 4);
        publisher.publish(transactionId, 5);

        // Return a response
        var transaction = new Transaction(transactionId);
        return new ResponseEntity<Transaction>(transaction, HttpStatus.OK);
    }
}