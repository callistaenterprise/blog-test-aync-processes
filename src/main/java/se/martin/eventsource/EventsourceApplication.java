package se.martin.eventsource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ServletComponentScan
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class EventsourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventsourceApplication.class, args);
    }

}
