package com.vivareal.logger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.vidageek.mirror.dsl.Mirror;

import org.apache.log4j.Priority;

public class KeyValueLogger implements Logger {

    private static final String DEFAULT_SEPARATOR = "=";
    private final org.apache.log4j.Logger logger;
    private String separator;


    private KeyValueLogger(org.apache.log4j.Logger logger) {
	this.logger = logger;
    }
    
    private KeyValueLogger(org.apache.log4j.Logger logger, String separator) {
	this.logger = logger;
	this.separator = null;
	this.separator = separator;
    }    

    public static KeyValueLogger getLogger(Class<?> clazz) {
	return new KeyValueLogger(org.apache.log4j.Logger.getLogger(clazz));
    }

    public static KeyValueLogger getLogger(Class<?> clazz, String separator) {
	return new KeyValueLogger(org.apache.log4j.Logger.getLogger(clazz), separator);
    }    
    
    
    @Override
    public void log(Priority priority, String message, Object... data) {
	logger.log(priority, getMessage(message, data));
    }

    @Override
    public void log(Priority priority, String message, Throwable t, Object... data) {
	logger.log(priority, getMessage(message, data), t);
    }    
    
    private String getMessage(String message, Object... data) {
	if (data == null || data.length == 0)
	    return message;
	
	return message.trim() + " " + serialize(data);
    }

    @SuppressWarnings("rawtypes")
    private String serialize(Object... data) {
	StringBuffer buffer = new StringBuffer();

	if (data != null && data.length > 0) {
	    for (Object object : data) {
		if (object instanceof Map) {
		    buffer.append(this.serializeMap((Map) object));
		} else {
		    buffer.append(this.serializeObject(object));
		}
	    }
	}
	return buffer.toString();
    }

    private Object serializeObject(Object object) {
	
	StringBuffer buffer = new StringBuffer();
	
	Mirror mirror = new Mirror();
	
	List<Field> fields = mirror.on(object.getClass()).reflectAll().fields();
	
	if (fields != null && fields.size() > 0) {
	    for (Field field : fields) {
		Object value = mirror.on(object).get().field(field.getName());
		if (value != null) {
		    buffer.append(formatKeyValue(field.getName(), value.toString()));
		}
	    }
	}
	
	return buffer.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String serializeMap(Map map) {
	
	StringBuffer buffer = new StringBuffer();
	
	if (map != null && map.size() > 0) {
	    for (Entry entry : (Set<Entry>) map.entrySet()) {
		if (entry.getValue() != null) {
		    buffer.append(formatKeyValue(entry.getKey(), entry.getValue()));
		}
	    }
	}
	
	return buffer.toString();
    }
    
    private String formatKeyValue(Object key, Object value) {
	
	StringBuffer buffer = new StringBuffer();
	buffer.append(" ");
	buffer.append(key.toString());
	buffer.append(this.separator != null ? this.separator : DEFAULT_SEPARATOR);
	buffer.append(value.toString());
	buffer.append(" ");
	return buffer.toString();
    }
    
}
