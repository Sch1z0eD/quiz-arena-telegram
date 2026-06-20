package com.quizarena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QuizArenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizArenaApplication.class, args);
    }
}
