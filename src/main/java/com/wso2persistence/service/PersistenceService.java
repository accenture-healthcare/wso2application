package com.wso2persistence.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.rulesengine.library.dto.response.GetRoleResponse;
import com.wso2persistence.strategy.DatabaseStrategy;

@Service
public class PersistenceService {
	private static final Logger logger = LogManager.getLogger(PersistenceService.class);
	private static final String CLASS_NAME = "PersistenceService";
	private static final String LOG_COSTANT = "[CLASS:{}] [METHOD:{}]: {}";
	private static final String FUNCTION_HL7PARSING = "hl7Parsing";

	@Autowired
	private DatabaseStrategy databaseStrategy;



	private boolean isValidFileExtension(MultipartFile file) {
		String extension = FilenameUtils.getExtension(file.getOriginalFilename());
		return "xls".equals(extension) || "xlsx".equals(extension);
	}

	@Transactional
	public Map<String, Integer> upload(String function, MultipartFile file, String enteSanitario, String ambito,
			String messageType, Integer version) throws IOException {
		final String method_name = "upload";

		if (databaseStrategy.existsByVersionAndFunctionAndAmbitoAndEnteSanitarioAndMessageType(version, function,
				ambito, enteSanitario, messageType)) {
			final String logMsg = "versione della regola per enteSanitario/ambito/funzione"
					+ (messageType != null ? "/messageType" : "") + " già presente";
			logger.error(LOG_COSTANT, CLASS_NAME, method_name, logMsg);
			throw new IllegalArgumentException(logMsg);
		}

		String extension = FilenameUtils.getExtension(file.getOriginalFilename());

		if ("drl".equals(extension)) {
			uploadDrlFile(file, function, enteSanitario, ambito, messageType, version);
		} else {
			final String logMsg = "Errore: il file deve essere di tipo xls/xlsx oppure drl";
			logger.error(LOG_COSTANT, CLASS_NAME, method_name, logMsg);
			throw new IllegalArgumentException(logMsg);
		}

		Map<String, Integer> versionMap = new HashMap<>();
		versionMap.put("Versione", version);
		return versionMap;
	}

	private void uploadDrlFile(MultipartFile file, String function, String enteSanitario, String ambito,
			String messageType, Integer version) throws IOException {
		final String method_name = "uploadDrlFile";
		Map<Integer, String> fileContent = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			int indice = 0;
			while ((line = br.readLine()) != null) {
				fileContent.put(indice, line);
				indice++;
			}
		} catch (IOException e) {
			final String logMsg = "Errore durante la lettura del file .drl -> " + e.getMessage();
			logger.error(LOG_COSTANT, CLASS_NAME, method_name, logMsg);
			throw e;
		}
		if (fileContent.isEmpty()) {
			final String logMsg = "il file .drl è vuoto e non contiene regole";
			logger.error(LOG_COSTANT, CLASS_NAME, method_name, logMsg);
			throw new IllegalArgumentException(logMsg);
		}

		databaseStrategy.writeDrlWithMongoDb(function, fileContent, enteSanitario, ambito, messageType, version);
	}

	public GetRoleResponse getRoles(String ambito, String enteSanitario, String function, String messageType,
			String file) {
		messageType = messageType.replace("'", "^");
		return databaseStrategy.getRuleString(ambito, enteSanitario, function, messageType, file);
	}

	@Transactional
	public void delete(String function, String enteSanitario, String ambito, String messageType, Integer version) {
		databaseStrategy.delete(function, enteSanitario, ambito, messageType, version);
	}

	
}
