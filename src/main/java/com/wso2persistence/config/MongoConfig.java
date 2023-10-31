package com.wso2persistence.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.connection.SslSettings;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

	@Value("${spring.data.mongodb.host}")
	private String host;

	@Value("${spring.data.mongodb.port}")
	private int port;

	@Value("${spring.data.mongodb.username}")
	private String username;

	@Value("${spring.data.mongodb.password}")
	private String password;

	@Value("${spring.data.mongodb.database}")
	private String database;

	@Value("${mongo.ssl.enabled}")
	private boolean sslEnabled;

	@Value("${mongo.ssl.invalidHostNameAllowed}")
	private boolean invalidHostNameAllowed;

	@Value("${mongo.ssl.pem}")
	private String pemLocation;

	@Value("${mongo.authenticationMechanisms}")
	private String authenticationMechanisms;

	@Value("${mongo.passwordLess}")
	private boolean passwordLess;

	private static final Logger logger = LogManager.getLogger(MongoConfig.class);
	private static final String LOG_COSTANT = "[CLASS:{}] [METHOD:{}]: {}";
	private static final String CLASS_NAME = "MongoConfig";

	@Override
	protected String getDatabaseName() {
		return database;
	}

	@Bean
	@Override
	public MongoClientSettings mongoClientSettings() {
		final String method_name = "mongoClientSettings";
		try {
			SslSettings sslSettings = null;

			String sslLog = "Proprietà SSLEANBLED: " + sslEnabled;
			logger.info(LOG_COSTANT, CLASS_NAME, method_name, sslLog);
			if (sslEnabled) {
				sslSettings = generateSslSettings();
			}

			final SslSettings finalSslSettings = sslSettings;

			MongoClientSettings.Builder clientBuilder = finalSslSettings != null
					? (MongoClientSettings.builder()
							.applyToSslSettings(builder -> builder.applySettings(finalSslSettings))
							.applyToClusterSettings(
									builder -> builder.hosts(Collections.singletonList(new ServerAddress(host, port)))))
					: (MongoClientSettings.builder().applyToClusterSettings(
							builder -> builder.hosts(Collections.singletonList(new ServerAddress(host, port)))));

			String clientLog = "MongoClientSettings utilizza host: " + host + " port:" + port;
			logger.info(LOG_COSTANT, CLASS_NAME, method_name, clientLog);

			if (!passwordLess) {
				MongoCredential credential = null;
				switch (authenticationMechanisms) {
				case "plain":
					credential = MongoCredential.createPlainCredential(username, database, password.toCharArray());
					break;
				case "SHA1":
					credential = MongoCredential.createScramSha1Credential(username, database, password.toCharArray());
					break;
				case "SHA256":
					credential = MongoCredential.createScramSha256Credential(username, database,
							password.toCharArray());
					break;
				case "X509":
					credential = MongoCredential.createMongoX509Credential(username);
					break;
				default:
					credential = MongoCredential.createCredential(username, database, password.toCharArray());
					break;
				}
				clientBuilder.credential(credential);
				String credentialLog = "PasswordLess = false , quindi utilizzo il Meccanismo di accesso: "
						+ credential.getMechanism() + " meccanismo di autenticazione: "
						+ credential.getAuthenticationMechanism() + " su username:" + username + " e database: "
						+ database;
				logger.info(LOG_COSTANT, CLASS_NAME, method_name, credentialLog);
			}

			return clientBuilder.build();
		} catch (Exception e) {
			throw new MongoException("Error configuring SSL for MongoDB", e);
		}
	}

	private SslSettings generateSslSettings() throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
			KeyManagementException, IOException {
		final String method_name = "generateSslSettings";
		File pemFile = new File(pemLocation);

		String pemLog = "Prelevo certificato .pem da: " + pemLocation;
		logger.info(LOG_COSTANT, CLASS_NAME, method_name, pemLog);

		X509Certificate preCert = null;
		try (FileInputStream pemInputStream = new FileInputStream(pemFile)) {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			preCert = (X509Certificate) certificateFactory.generateCertificate(pemInputStream);
			logger.info(LOG_COSTANT, CLASS_NAME, method_name, "Algoritmo del certificato utilizzato: X.509");
		} catch (IOException e) {
			final String logMsg = "Certificato Mongo non trovato o non leggibile => path:" + pemLocation;
			logger.error(LOG_COSTANT, CLASS_NAME, method_name, logMsg);
			throw e;
		}

		final X509Certificate cert = preCert;
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init((KeyStore) null);
		X509TrustManager trustManager = new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				for (X509Certificate certificate : arg0) {
					if (certificate.equals(cert)) {
						return;
					}
				}
				throw new SecurityException("Server certificate not trusted");
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String arg1) throws CertificateException {
				for (X509Certificate certificate : certs) {
					if (certificate.equals(cert)) {
						return;
					}
				}
				throw new SecurityException("Server certificate not trusted");
			}
		};

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new X509TrustManager[] { trustManager }, null);

		String tlsLog = "TLS configurato correttamente, con invalidHostNameAllowed: " + invalidHostNameAllowed;
		logger.info(LOG_COSTANT, CLASS_NAME, method_name, tlsLog);

		return SslSettings.builder().enabled(sslEnabled).invalidHostNameAllowed(invalidHostNameAllowed)
				.context(sslContext).build();
	}

	@Override
	public void configureClientSettings(MongoClientSettings.Builder builder) {
		builder.applyToClusterSettings(
				settingsBuilder -> settingsBuilder.hosts(Collections.singletonList(new ServerAddress(host, port))));
	}
}