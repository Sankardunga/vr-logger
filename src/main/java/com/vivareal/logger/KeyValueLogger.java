package com.vivareal.logger;


public class KeyValueLogger extends AbstractLogger implements Logger {

    private static final String DEFAULT_SEPARATOR = "=";
    private String separator;
    
    private KeyValueLogger(org.apache.log4j.Logger logger) {
	super(logger);
	this.separator = DEFAULT_SEPARATOR;
    }
    
    private KeyValueLogger(org.apache.log4j.Logger logger, String separator) {
	this(logger);
	this.separator = separator;
    }    

    public static KeyValueLogger getLogger(Class<?> clazz) {
	return new KeyValueLogger(org.apache.log4j.Logger.getLogger(clazz));
    }

    public static KeyValueLogger getLogger(Class<?> clazz, String separator) {
	return new KeyValueLogger(org.apache.log4j.Logger.getLogger(clazz), separator);
    }    
    
    
    
    @Override
    public LoggerDataBuilder with(String key) {
	return new KeyValueLoggerDataBuilder(logger, separator, key);
    }

    @Override
    public LogDataConjunction withObject(Object object) {
	return new KeyValueLoggerDataBuilder(logger, separator, object);
    }
    
    @Override
    public LogDataConjunction withObject(Object object, String prefix) {
	return new KeyValueLoggerDataBuilder(logger, separator, object, prefix);
    }    

    @Override
    protected String getFullMessage(String message) {
	String fullMessage = message.trim() + " logLevel" + this.separator + level.toString();
	
	System.out.println("Olar! " + fullMessage);
	
	return fullMessage;
    }
}
