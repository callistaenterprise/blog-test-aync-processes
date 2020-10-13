package se.martin.eventsource.dosomething;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@AllArgsConstructor
@Getter
@ToString
class Transaction {

    private UUID transactionId;
}
