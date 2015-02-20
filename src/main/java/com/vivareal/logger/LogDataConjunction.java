package com.vivareal.logger;

public interface LogDataConjunction extends BaseLogger {

    LoggerDataBuilder and(String key);
    
    LogDataConjunction andObject(Object object);
    
    LogDataConjunction andObject(Object object, String prefix);
    
}
