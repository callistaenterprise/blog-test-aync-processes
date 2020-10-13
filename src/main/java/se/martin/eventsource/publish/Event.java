package se.martin.eventsource.publish;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@AllArgsConstructor
@Getter
@ToString
public class Event {

    private Metadata metadata;

    private UUID transactionId;

    private Integer sequenceId;

    private String padding;

}