package com.example.petapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

final class OracleWalletBootstrap {

	private static final String BUCKET_ENV = "ORACLE_WALLET_S3_BUCKET";
	private static final String KEY_ENV = "ORACLE_WALLET_S3_KEY";

	private OracleWalletBootstrap() {
	}

	static void loadFromEnvironment() {
		String bucket = System.getenv(BUCKET_ENV);
		String key = System.getenv(KEY_ENV);
		boolean hasBucket = bucket != null && !bucket.isBlank();
		boolean hasKey = key != null && !key.isBlank();

		if (!hasBucket && !hasKey) {
			return;
		}
		if (!hasBucket || !hasKey) {
			throw new IllegalStateException(BUCKET_ENV + " and " + KEY_ENV + " must both be configured.");
		}

		String region = System.getenv("AWS_REGION");
		if (region == null || region.isBlank()) {
			region = System.getenv("AWS_DEFAULT_REGION");
		}
		if (region == null || region.isBlank()) {
			throw new IllegalStateException("AWS_REGION must be configured to load the Oracle wallet from S3.");
		}

		String walletDirectory = System.getenv("TNS_ADMIN");
		if (walletDirectory == null || walletDirectory.isBlank()) {
			walletDirectory = "/app/wallet";
		}
		loadFromS3(bucket, key, Region.of(region), Path.of(walletDirectory));
	}

	static void loadFromS3(String bucket, String key, Region region, Path walletDirectory) {
		Path stagingDirectory = null;
		try {
			stagingDirectory = Files.createTempDirectory("oracle-wallet-");
			try (S3Client s3 = S3Client.builder().region(region).build();
					ResponseInputStream<GetObjectResponse> object = s3.getObject(
							GetObjectRequest.builder().bucket(bucket).key(key).build())) {
				extractWallet(object, stagingDirectory);
			}
			copyWalletFiles(stagingDirectory, walletDirectory);
		}
		catch (IOException | S3Exception exception) {
			throw new IllegalStateException("Unable to load the Oracle wallet from S3.", exception);
		}
		finally {
			if (stagingDirectory != null) {
				deleteStagingDirectory(stagingDirectory);
			}
		}
	}

	static void extractWallet(InputStream archive, Path stagingDirectory) throws IOException {
		Path root = stagingDirectory.toAbsolutePath().normalize();
		Set<Path> extractedPaths = new HashSet<>();
		boolean hasTnsNames = false;

		try (ZipInputStream zip = new ZipInputStream(archive)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String entryName = entry.getName();
				if (entryName.isBlank() || entryName.startsWith("/") || entryName.contains("\\")) {
					throw new IOException("The Oracle wallet archive contains an unsafe path.");
				}

				Path relativePath = Path.of(entryName).normalize();
				Path destination = root.resolve(relativePath).normalize();
				if (relativePath.isAbsolute() || relativePath.startsWith("..") || !destination.startsWith(root)
						|| !extractedPaths.add(relativePath)) {
					throw new IOException("The Oracle wallet archive contains an unsafe or duplicate path.");
				}

				if (relativePath.equals(Path.of("tnsnames.ora")) && !entry.isDirectory()) {
					hasTnsNames = true;
				}
				if (entry.isDirectory()) {
					Files.createDirectories(destination);
				}
				else {
					Path parent = destination.getParent();
					if (parent != null) {
						Files.createDirectories(parent);
					}
					try (var output = Files.newOutputStream(destination)) {
						zip.transferTo(output);
					}
				}
				zip.closeEntry();
			}
		}

		if (!hasTnsNames) {
			throw new IOException("The Oracle wallet archive does not contain a root-level tnsnames.ora.");
		}
	}

	private static void copyWalletFiles(Path stagingDirectory, Path walletDirectory) throws IOException {
		Path destinationRoot = walletDirectory.toAbsolutePath().normalize();
		Files.createDirectories(destinationRoot);
		try (Stream<Path> paths = Files.walk(stagingDirectory)) {
			for (Path source : paths.toList()) {
				if (source.equals(stagingDirectory)) {
					continue;
				}
				Path destination = destinationRoot.resolve(stagingDirectory.relativize(source)).normalize();
				if (!destination.startsWith(destinationRoot)) {
					throw new IOException("The Oracle wallet archive contains an unsafe path.");
				}
				if (Files.isDirectory(source)) {
					Files.createDirectories(destination);
				}
				else {
					Path parent = destination.getParent();
					if (parent != null) {
						Files.createDirectories(parent);
					}
					Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static void deleteStagingDirectory(Path directory) {
		try (Stream<Path> paths = Files.walk(directory)) {
			for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
				Files.deleteIfExists(path);
			}
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to clean up temporary Oracle wallet files.", exception);
		}
	}

}
