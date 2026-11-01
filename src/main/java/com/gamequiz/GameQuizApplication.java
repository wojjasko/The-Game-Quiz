package com.gamequiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GameQuizApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameQuizApplication.class, args);
    }
}