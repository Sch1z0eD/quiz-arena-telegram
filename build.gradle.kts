plugins {
    java
    id("org.springframework.boot") version "3.5.15"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.quizarena"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:10.0.0")
    implementation("org.telegram:telegrambots-client:10.0.0")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.19")
    implementation("org.apache.xmlgraphics:batik-codec:1.19")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
