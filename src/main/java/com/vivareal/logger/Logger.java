package com.vivareal.logger;

public interface Logger extends BaseLogger {

    LoggerDataBuilder with(String key);
    
    LogDataConjunction withObject(Object object);
    
    LogDataConjunction withObject(Object object, String prefix);

}