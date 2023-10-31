package com.wso2persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.rulesengine.library.entity.LastExcelFile;

public interface LastExcelFileRepository extends MongoRepository<LastExcelFile, String> {

	Optional<LastExcelFile> findByFunctionAndEnteSanitarioAndAmbitoAndMessageType(String function, String enteSanitario,
			String ambito, String messageType);

	boolean existsByFunctionAndEnteSanitarioAndAmbitoAndMessageType(String function, String enteSanitario,
			String ambito, String messageType);

	void deleteByFunctionAndEnteSanitarioAndAmbitoAndMessageType(String function, String enteSanitario, String ambito,
			String messageType);

	Optional<LastExcelFile> findByNomeFileAndMessageType(String file, String messageType);
}
