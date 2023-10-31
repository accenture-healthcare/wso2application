package com.wso2persistence.strategy;

import java.util.Map;

import com.rulesengine.library.dto.response.GetRoleResponse;

public interface DatabaseStrategy {
       
    void writeDrlWithMongoDb(String function, Map<Integer,String> rules, String enteSanitario, String ambito, String messageType, Integer version);
    
    GetRoleResponse getRuleString(String ambito, String enteSanitario, String function, String messageType, String file);
    
    void delete(String function, String enteSanitario, String ambito, String messageType, Integer version);
    
    boolean existsByVersionAndFunctionAndAmbitoAndEnteSanitarioAndMessageType(Integer version, String function, String ambito, String enteSanitario, String messageType);

}
