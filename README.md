**EventSource Demo**

This demo is intended as an illustration for integration testing asynchronous behaviour in a Spring Boot application.

---

## Prerequisites

To run this application locally you will need:
* A Java runtime environment (JRE) version 11 or higher.
* Docker and Docker Compose

## Usage

To build the project and run the unit tests use `/gradlew build`.

This application requires a Kafka broker to be available. Although it is perfectly okay to start 
the application using `./gradlew bootRun` the resulting service will not work as desired.

To start the application with all dependencies use `./gradlew startServices`. 
To stop everything use `./gradlew stopServices`.

To run the integration tests use `/gradlew integrationTest`. This will start the dependencies, the service,
run the tests and then shut down all the services.

The integration test task sends Http requests to the eventsource application and will default to a host of `localhost:8097`.
If an alternative host is required the Gradle project parameter `eventsource.host` can be set. Similarly to
provide an alternative host for the Kafka broker set the Gradle project parameter `kafka.host` (defults to `localhost:19092`) 

For example:
```
./gradlew integrationTest -Peventsource.host=172.17.0.1:8097 -Pkafka.host=172.17.0.1:19092
```

## Structure

This project contains three sourcesets:
* /src/main/ - contains the Spring Boot application including the endpoint and a Kafka publisher.
* /src/test/ - contains unit tests. This type of test is out of scope for this application.
* /src/it/ - contains integration tests.

## Endpoints

If you start the application as a standalone app it is accessible on port 8080.
If you start the application in a docker container then it is accessible on port 8097.

Once the application is started you can access the "do something" endpoint:

```
curl -X POST http://localhost:8097/dosomething
```

## Healthcheck

The spring boot application comes packaged with the actuator. The healthcheck can be accessed using:

```
curl http://localhost:8097/actuator/health
```

## Features

To provoke some problems this project has some features...

Events published from the Spring Boot application contain very little useable data. 
To simulate a more realistic scenario the events contain a `padding` field which is just a large string.