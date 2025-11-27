package com.example.proyecto_pi3_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProyectoPi3BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProyectoPi3BackendApplication.class, args);
    }

}
