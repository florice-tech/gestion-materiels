package com.example.gestionmateriels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionMaterielsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionMaterielsApplication.class, args);
	}

}
