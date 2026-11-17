package com.example.petapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetApiApplication {

	public static void main(String[] args) {
		OracleWalletBootstrap.loadFromEnvironment();
		String databaseUrl = System.getenv("DB_URL");
		if (databaseUrl != null && !databaseUrl.isBlank()) {
			System.setProperty("DB_URL", OracleJdbcUrl.normalize(databaseUrl, System.getenv("TNS_ADMIN")));
		}
		SpringApplication.run(PetApiApplication.class, args);
	}

}
