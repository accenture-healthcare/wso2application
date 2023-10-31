package com.wso2persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.rulesengine.library.entity.DrlFile;

public interface DrlFileRepository extends MongoRepository<DrlFile, String> {
	Optional<DrlFile> findByFunctionAndAmbitoAndEnteSanitarioAndMessageTypeAndVersion(String function, String ambito, String messageType,
			String enteSanitario, Integer version);

	Optional<DrlFile> findTopByFunctionAndAmbitoAndEnteSanitarioAndMessageType(String function, String ambito, String enteSanitario, String messageType);

	Optional<DrlFile> findByFunctionAndAmbitoAndEnteSanitarioAndMessageType(String function, String ambito, String enteSanitario, String messageType);

	boolean existsByVersionAndFunctionAndAmbitoAndEnteSanitarioAndMessageType(Integer version, String function, String ambito, String enteSanitario,
			String messageType);
}
