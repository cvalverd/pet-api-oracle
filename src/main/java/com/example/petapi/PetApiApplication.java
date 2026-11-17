package com.example.petapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetApiApplication {

	public static void main(String[] args) {
		OracleWalletBootstrap.loadFromEnvironment();
		SpringApplication.run(PetApiApplication.class, args);
	}

}
