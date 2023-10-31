package com.wso2persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.rulesengine.library.entity.DrlFileDeleted;

public interface DrlFileDeletedRepository extends MongoRepository<DrlFileDeleted, String> {
}
