package com.vivareal.logger;

import org.apache.log4j.Priority;

public interface Logger {

    void log(Priority priority, String message, Object... data);
    
    void log(Priority priority, String message, Throwable t, Object... data);
    
}