package com.example.petapi;

final class OracleJdbcUrl {

	private static final String JDBC_PREFIX = "jdbc:oracle:thin:@";
	private static final String DEFAULT_TNS_ADMIN = "/app/wallet";

	private OracleJdbcUrl() {
	}

	static String normalize(String databaseUrl, String tnsAdmin) {
		if (databaseUrl == null || databaseUrl.isBlank()) {
			throw new IllegalArgumentException("DB_URL must be configured.");
		}

		String configuredUrl = databaseUrl.trim();
		if (configuredUrl.startsWith("jdbc:oracle:")) {
			return configuredUrl;
		}
		if (!configuredUrl.matches("[A-Za-z0-9][A-Za-z0-9_.-]*")) {
			throw new IllegalArgumentException("DB_URL must be an Oracle JDBC URL or a TNS alias.");
		}

		String walletDirectory = tnsAdmin == null || tnsAdmin.isBlank()
				? DEFAULT_TNS_ADMIN
				: tnsAdmin.trim();
		return JDBC_PREFIX + configuredUrl + "?TNS_ADMIN=" + walletDirectory;
	}

}
