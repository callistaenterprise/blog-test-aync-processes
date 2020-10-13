FROM adoptopenjdk:11-jre-openj9
ADD build/libs/*.jar /opt/eventsource/eventsource.jar

ENTRYPOINT ["java","-jar","/opt/eventsource/eventsource.jar"]