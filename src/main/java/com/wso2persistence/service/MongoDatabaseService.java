package com.wso2persistence.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.rulesengine.library.dto.response.GetRoleResponse;
import com.rulesengine.library.entity.DmnFile;
import com.rulesengine.library.entity.DrlFile;
import com.rulesengine.library.entity.DrlFileDeleted;
import com.rulesengine.library.entity.LastExcelFile;
import com.rulesengine.library.model.SaveLastExcelFileModel;
import com.wso2persistence.repository.DrlFileDeletedRepository;
import com.wso2persistence.repository.DrlFileRepository;
import com.wso2persistence.repository.LastExcelFileRepository;
import com.wso2persistence.strategy.DatabaseStrategy;

@Service
@Primary
public class MongoDatabaseService implements DatabaseStrategy {
	private static final Logger logger = LogManager.getLogger(MongoDatabaseService.class);
	private static final String LOG_COSTANT = "[CLASS:{}] [METHOD:{}]: {}";
	private static final String CLASS_NAME = "MongoDatabaseService";

	@Autowired
	private DrlFileRepository drlFileRepository;

	@Autowired
	private DrlFileDeletedRepository drlFileDeletedRepository;

	@Autowired
	private LastExcelFileRepository lastExcelFileRepository;


	@Transactional
	public void writeDrlWithMongoDb(String function, Map<Integer, String> rules, String enteSanitario, String ambito,
			String messageType, Integer version) {
		DrlFile drlFile = drlFileRepository
				.findByFunctionAndAmbitoAndEnteSanitarioAndMessageType(function, ambito, enteSanitario, messageType)
				.orElse(new DrlFile());

		if (drlFile.getId() != null) {
			DrlFileDeleted drlFileDeleted = new DrlFileDeleted(drlFile);
			drlFileDeleted.setDataCancellazione(new Date());
			drlFileDeletedRepository.save(drlFileDeleted);
		} else {
			drlFile.setDataCreazione(new Date());
		}

		List<String> regole = new ArrayList<>();

		int maxKey = Collections.max(rules.keySet());

		for (int i = 0; i <= maxKey; i++) {
			regole.add(null);
		}

		for (Map.Entry<Integer, String> entry : rules.entrySet()) {
			int index = entry.getKey();
			String value = entry.getValue();
			regole.set(index, value);
		}

		regole.removeIf(Objects::isNull);

		drlFile.setRules(regole);
		drlFile.setVersion(version);
		drlFile.setFunction(function);
		drlFile.setEnteSanitario(enteSanitario);
		drlFile.setAmbito(ambito);
		drlFile.setMessageType(messageType);
		drlFileRepository.save(drlFile);
	}

	public GetRoleResponse getRuleString(String ambito, String enteSanitario, String function, String messageType,
			String file) {
		final String method_name = "getRuleString";

		final String pickLogMsg = "Prelevo la regola per function:" + function + " ambito:" + ambito
				+ "   enteSanitario:" + enteSanitario
				+ (file == null ? " messageType:" + messageType : " file: " + file);
		logger.info(LOG_COSTANT, CLASS_NAME, method_name, pickLogMsg);

		if (file != null) {
			return getRuleStringFromFile(file,messageType);
		} else {
			return getRuleStringWithoutFile(messageType,function,ambito,enteSanitario);
		}
	}

	private GetRoleResponse getRuleStringWithoutFile(String messageType,
			String function, String ambito, String enteSanitario) {
		final String method_name = "getRuleStringWithoutFile";

		DrlFile drlFile = drlFileRepository
				.findByFunctionAndAmbitoAndEnteSanitarioAndMessageType(function, ambito, enteSanitario, messageType)
				.orElse(null);

		if (drlFile == null) {
			String[] messageTypeArray = messageType.split("\\^");
			StringBuilder newMessageType = new StringBuilder();

			if (messageTypeArray.length > 1) {
				newMessageType.append(messageTypeArray[0]).append("^").append(messageTypeArray[1]);
				drlFile = drlFileRepository.findByFunctionAndAmbitoAndEnteSanitarioAndMessageType(function, ambito,
						enteSanitario, newMessageType.toString()).orElse(null);
			}

			if (drlFile == null && messageTypeArray.length > 0) {
				newMessageType.setLength(0); // Pulisci il StringBuilder
				newMessageType.append(messageTypeArray[0]);
				drlFile = drlFileRepository.findByFunctionAndAmbitoAndEnteSanitarioAndMessageType(function, ambito,
						enteSanitario, newMessageType.toString()).orElse(null);
			}
		}

		List<String> rules = drlFile != null ? drlFile.getRules() : new ArrayList<>();

		LastExcelFile lastExcelFile = lastExcelFileRepository
				.findByFunctionAndEnteSanitarioAndAmbitoAndMessageType(function, enteSanitario, ambito,
						drlFile != null ? drlFile.getMessageType() : messageType)
				.orElse(null);

		String nomeFile = "";

		if (lastExcelFile != null)
			nomeFile = lastExcelFile.getNomeFile();

		final String pickedLogMsg = "Ho prelevato la regola per function:" + function + " ambito:" + ambito
				+ " enteSanitario:" + enteSanitario + " messageType: " + messageType + " ed ho trovato versione: "
				+ (drlFile == null ? "0" : drlFile.getVersion()) + " con nomeFile: " + nomeFile
				+ " e messageTypeFile: " + (drlFile != null ? drlFile.getMessageType() : messageType);
		logger.info(LOG_COSTANT, CLASS_NAME, method_name, pickedLogMsg);
		return new GetRoleResponse(generateRuleDrl(rules), drlFile != null ? drlFile.getVersion() : 0, nomeFile);
	}

	private GetRoleResponse getRuleStringFromFile(String file, String messageType) {
		final String method_name = "getRuleStringFromFile";
		LastExcelFile lastExcelFile = lastExcelFileRepository.findByNomeFileAndMessageType(file, messageType)
				.orElse(null);
		if (lastExcelFile == null) {
			final String logMsg = "Nessun file presente con nome: " + file + " e messageType: " + messageType;
			logger.error(LOG_COSTANT, CLASS_NAME, method_name , logMsg);
			throw new IllegalArgumentException(logMsg);
		}
		DrlFile drlFile = drlFileRepository.findByFunctionAndAmbitoAndEnteSanitarioAndMessageTypeAndVersion(
				lastExcelFile.getFunction(), lastExcelFile.getAmbito(), lastExcelFile.getEnteSanitario(),
				lastExcelFile.getMessageType(), lastExcelFile.getVersion()).orElse(null);
		if (drlFile == null) {
			final String logMsg = "Nessun file DRL associato al file: " + file + " con function: "
					+ lastExcelFile.getFunction() + " con ambito: " + lastExcelFile.getAmbito() + " enteSanitario:"
					+ lastExcelFile.getEnteSanitario() + "  messageType:" + lastExcelFile.getMessageType()
					+ " version: " + lastExcelFile.getVersion();
			logger.error(LOG_COSTANT, CLASS_NAME, method_name, logMsg);
			throw new IllegalArgumentException(logMsg);
		}
		List<String> rules = drlFile.getRules();

		String nomeFile = lastExcelFile.getNomeFile();

		final String pickedLogMsg = "Ho prelevato la regola per file: " + file + " con function: "
				+ lastExcelFile.getFunction() + " con ambito: " + lastExcelFile.getAmbito() + "  enteSanitario:"
				+ lastExcelFile.getEnteSanitario() + " messageType: " + lastExcelFile.getMessageType()
				+ " version: " + lastExcelFile.getVersion();
		logger.info(LOG_COSTANT, CLASS_NAME, method_name, pickedLogMsg);

		return new GetRoleResponse(generateRuleDrl(rules), drlFile.getVersion(), nomeFile);
	}

	private String generateRuleDrl(List<String> rules) {
		if (rules != null && !rules.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String item : rules) {
				sb.append(item).append(System.lineSeparator());
			}
			return sb.toString();
		} else
			return "";
	}

	@Transactional
	public void delete(String function, String enteSanitario, String ambito, String messageType, Integer version) {
		final String method_name = "delete";
		DrlFile drlFile = drlFileRepository.findByFunctionAndAmbitoAndEnteSanitarioAndMessageTypeAndVersion(function,
				ambito, enteSanitario, messageType, version).orElse(null);
		if (drlFile == null) {
			final String logMsg = "Nessuna regola trovata per questo ambito/enteSanitario/funzione/versione/messageType";
			logger.error(LOG_COSTANT, CLASS_NAME, method_name, logMsg);
			throw new IllegalArgumentException(logMsg);
		}

		DrlFileDeleted drlFileDeleted = new DrlFileDeleted(drlFile);
		drlFileDeleted.setDataCancellazione(new Date());
		drlFileDeletedRepository.save(drlFileDeleted);
		drlFileRepository.delete(drlFile);

		if (lastExcelFileRepository.existsByFunctionAndEnteSanitarioAndAmbitoAndMessageType(function, enteSanitario,
				ambito, messageType)) {
			lastExcelFileRepository.deleteByFunctionAndEnteSanitarioAndAmbitoAndMessageType(function, enteSanitario,
					ambito, messageType);
		}

	}

	public boolean existsByVersionAndFunctionAndAmbitoAndEnteSanitarioAndMessageType(Integer version, String function,
			String ambito, String enteSanitario, String messageType) {
		return drlFileRepository.existsByVersionAndFunctionAndAmbitoAndEnteSanitarioAndMessageType(version, function,
				ambito, enteSanitario, messageType);
	}

	
}
