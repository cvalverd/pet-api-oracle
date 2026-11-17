package com.example.petapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class OracleWalletBootstrapTest {

	@TempDir
	Path tempDirectory;

	@Test
	void extractsWalletWithRootLevelTnsNames() throws IOException {
		Path stagingDirectory = Files.createDirectory(tempDirectory.resolve("staging"));

		OracleWalletBootstrap.extractWallet(walletArchive(Map.of(
				"tnsnames.ora", "wallet connection",
				"cwallet.sso", "wallet credential")), stagingDirectory);

		assertEquals("wallet connection", Files.readString(stagingDirectory.resolve("tnsnames.ora")));
		assertEquals("wallet credential", Files.readString(stagingDirectory.resolve("cwallet.sso")));
	}

	@Test
	void rejectsArchiveWithPathTraversal() throws IOException {
		Path stagingDirectory = Files.createDirectory(tempDirectory.resolve("staging"));

		assertThrows(IOException.class, () -> OracleWalletBootstrap.extractWallet(walletArchive(Map.of(
				"tnsnames.ora", "wallet connection",
				"../escaped.txt", "outside")), stagingDirectory));

		assertFalse(Files.exists(tempDirectory.resolve("escaped.txt")));
	}

	@Test
	void rejectsArchiveWithoutRootLevelTnsNames() throws IOException {
		Path stagingDirectory = Files.createDirectory(tempDirectory.resolve("staging"));

		assertThrows(IOException.class, () -> OracleWalletBootstrap.extractWallet(walletArchive(Map.of(
				"nested/tnsnames.ora", "wallet connection")), stagingDirectory));
	}

	@Test
	void buildsS3ClientWithTheDefaultRuntimeConfiguration() {
		try (S3Client ignored = S3Client.builder().region(Region.US_EAST_1).build()) {
			assertNotNull(ignored);
		}
	}

	private ByteArrayInputStream walletArchive(Map<String, String> entries) throws IOException {
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(archive)) {
			for (Map.Entry<String, String> entry : entries.entrySet()) {
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue().getBytes());
				zip.closeEntry();
			}
		}
		return new ByteArrayInputStream(archive.toByteArray());
	}

}
