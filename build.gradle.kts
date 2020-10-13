plugins {
    java
    id("org.springframework.boot") version "2.3.2.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

group = "se.martin"
version = "1.0.0"

repositories {
    mavenCentral()
}

// Add source set for integration tests
java {
    sourceSets.create("integrationTest") {
        java.srcDir("src/it/java")
        resources.srcDirs("src/it/resources")
    }
}

val integrationTestCompileOnly by configurations.getting {
    extendsFrom(configurations.compileOnly.get())
}
val integrationTestAnnotationProcessor by configurations.getting {
    extendsFrom(configurations.annotationProcessor.get())
}
val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
}

dependencies {

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth:2.2.5.RELEASE")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")

    integrationTestCompileOnly("org.projectlombok:lombok")
    integrationTestAnnotationProcessor("org.projectlombok:lombok")

    integrationTestImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    integrationTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")

    integrationTestImplementation("org.apache.kafka:kafka-clients:2.5.0")
    integrationTestImplementation("com.jayway.jsonpath:json-path:2.4.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}

task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    useJUnitPlatform()

    minHeapSize = "512m"
    maxHeapSize = "1024m"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter("test")
    dependsOn("startServices")
    finalizedBy("stopServices")

    // Enable parallel testing in JUnit5 with a fixed thread pool
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "100")

    // Setup JMX for integration test JVM
    systemProperty("com.sun.management.jmxremote.port","3333")
    systemProperty("com.sun.management.jmxremote.ssl","false")
    systemProperty("com.sun.management.jmxremote.authenticate","false")

    // Set eventsource.host property as integration test JVM property if set
    project.findProperty("eventsource.host")?.let { systemProperty("eventsource.host", it) }
    project.findProperty("kafka.host")?.let { systemProperty("kafka.host", it) }

}

task<Exec>("buildImage") {
    group = "build"
    description = "Builds a docker inage containing the application"

    dependsOn("assemble")
    mustRunAfter("test")

    workingDir("$projectDir")
    commandLine("docker", "build", "-t", "martin/eventsource", ".")
}

task<Exec>("startServices") {
    group = "verification"
    description = "Starts the application and dependent resources"

    dependsOn("buildImage")

    workingDir("$projectDir")

    project.findProperty("kafka.host")?.let { environment.set("LISTENER_HOST", it) }

    commandLine("docker-compose", "up", "-d")
}

task<Exec>("stopServices") {
    group = "verification"
    description = "Stops the application and dependent resources"

    workingDir("$projectDir")
    commandLine("docker-compose", "down")
}
