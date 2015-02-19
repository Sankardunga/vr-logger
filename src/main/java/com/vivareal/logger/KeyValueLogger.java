package com.vivareal.logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.vidageek.mirror.dsl.Mirror;

public class KeyValueLogger extends AbstractLogger implements Logger, LogDataBuilder, LogDataConjunction {

    private static final String DEFAULT_SEPARATOR = "=";
    private String separator;
    
    private static ThreadLocal<Map<String, String>> keyValueMap;
    private static ThreadLocal<Map<String, Object>> objects;
    private static ThreadLocal<String> currentKey;

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
    public LogDataBuilder with(String key) {
	if (currentKey == null) {
	    currentKey = new ThreadLocal<String>();
	}
	currentKey.set(key);
	
	return this;
    }

    @Override
    public LogDataConjunction withObject(Object object) {
	String qualifiedName = object.getClass().getName();
	String prefix = qualifiedName.substring(qualifiedName.lastIndexOf(".")+1);
	
	return this.withObject(object, prefix.replaceFirst(new Character(prefix.charAt(0)).toString(), 
		new Character(prefix.charAt(0)).toString().toLowerCase()));
    }
    
    @Override
    public LogDataConjunction withObject(Object object, String prefix) {
	if (objects == null) {
	    objects = new ThreadLocal<Map<String, Object>>();
	    objects.set(new HashMap<String, Object>());
	}
	objects.get().put(prefix, object);
	return this;
    }    
    
    @Override
    public LogDataConjunction value(Object value) {
	if (keyValueMap == null) {
	    keyValueMap = new ThreadLocal<Map<String,String>>();
	    keyValueMap.set(new HashMap<String, String>());
	}
	keyValueMap.get().put(currentKey.get(), value.toString());
	return this;
    }

    @Override
    public LogDataBuilder and(String key) {
	return this.with(key);
    }

    @Override
    public LogDataConjunction andObject(Object object) {
	return this.withObject(object);
    }

    @Override
    public LogDataConjunction andObject(Object object, String prefix) {
	return this.withObject(object, prefix);
    }    
    
    protected String getFullMessage(String message) {
	if (!hasSerializableData())
	    return message;
	
	StringBuffer buffer = new StringBuffer(message.trim());
	
	
	if (hasSerializableObjects()) {
	    buffer.append(" ");
	    buffer.append(serialize(objects.get()));
	    objects.get().clear();
	}
	
	if (hasSerializableMap()) {
	    buffer.append(" ");
	    buffer.append(serializeMap(keyValueMap.get()));
	    keyValueMap.get().clear();
	}
	
	buffer.append(" ");
	buffer.append(getLogLevelAttribute());	
	
	System.out.println(buffer);
	return buffer.toString();
    }

    private String getLogLevelAttribute() {
	return "logLevel" + this.separator + this.level.toString();
    }

    private boolean hasSerializableData() {
	return hasSerializableMap() || hasSerializableObjects();
    }

    private boolean hasSerializableObjects() {
	return objects != null && objects.get().size() > 0;
    }

    private boolean hasSerializableMap() {
	return keyValueMap != null && keyValueMap.get().size() > 0;
    }

    @SuppressWarnings("rawtypes")
    private String serialize(Map<String, Object> objects) {
	StringBuffer buffer = new StringBuffer();

	if (objects != null && objects.size() > 0) {
	    for (Entry<String, Object> entry : objects.entrySet()) {
		if (entry.getValue() instanceof Map) {
		    buffer.append(this.serializeMap((Map) entry.getValue()));
		} else {
		    buffer.append(this.serializeObject(entry));
		}
	    }
	}
	return buffer.toString();
    }

    private String serializeObject(Entry<String, Object> entry) {
	
	StringBuffer buffer = new StringBuffer();

	Object object = entry.getValue();
	String prefix = entry.getKey();
	
	Mirror mirror = new Mirror();
	
	List<Field> fields = mirror.on(object.getClass()).reflectAll().fields();
	
	if (fields != null && fields.size() > 0) {
	    for (Field field : fields) {
		Object value = mirror.on(object).get().field(field.getName());
		if (value != null) {
		    buffer.append(formatKeyValue(prefix + "." + field.getName(), value.toString()));
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
	buffer.append(this.separator);
	buffer.append(value.toString());
	buffer.append(" ");
	return buffer.toString();
    }

}
