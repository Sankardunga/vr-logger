package com.vivareal.logger;

public interface Logger extends BaseLogger {

    LogDataBuilder with(String key);
    
    LogDataConjunction withObject(Object object);
    
    LogDataConjunction withObject(Object object, String prefix);

}