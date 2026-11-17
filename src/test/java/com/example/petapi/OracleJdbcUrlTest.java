package com.example.petapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OracleJdbcUrlTest {

	@Test
	void convertsTnsAliasToWalletBackedJdbcUrl() {
		assertEquals("jdbc:oracle:thin:@petdb_tp?TNS_ADMIN=/app/wallet",
				OracleJdbcUrl.normalize(" petdb_tp ", null));
	}

	@Test
	void usesConfiguredTnsAdminDirectoryForAlias() {
		assertEquals("jdbc:oracle:thin:@petdb_tp?TNS_ADMIN=C:/oracle/wallet",
				OracleJdbcUrl.normalize("petdb_tp", "C:/oracle/wallet"));
	}

	@Test
	void leavesFullJdbcUrlUnchanged() {
		String jdbcUrl = "jdbc:oracle:thin:@petdb_tp?TNS_ADMIN=/custom/wallet";

		assertEquals(jdbcUrl, OracleJdbcUrl.normalize(jdbcUrl, "/app/wallet"));
	}

	@Test
	void rejectsInvalidTnsAlias() {
		assertThrows(IllegalArgumentException.class, () -> OracleJdbcUrl.normalize("petdb_tp?unexpected=true", null));
	}

}
