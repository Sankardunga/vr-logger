package com.vivareal.logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.vidageek.mirror.dsl.Mirror;

import org.apache.log4j.Logger;

public class KeyValueLoggerDataBuilder extends AbstractLogger implements LoggerDataBuilder, LogDataConjunction {

    private Map<String, String> keyValueMap;
    private Map<String, Object> objects;
    private String currentKey;
    private String separator;

    private KeyValueLoggerDataBuilder(Logger logger, String separator) {
	super(logger);
	this.separator = separator;
	this.keyValueMap = new HashMap<String, String>();
	this.objects = new HashMap<String, Object>();
    }
    
    protected KeyValueLoggerDataBuilder(Logger logger, String separator, String key) {
	this(logger, separator);
	this.and(key);
    }
    
    protected KeyValueLoggerDataBuilder(Logger logger, String separator, Object object) {
	this(logger, separator);
	this.andObject(object);
    }
    
    protected KeyValueLoggerDataBuilder(Logger logger, String separator, Object object, String prefix) {
	this(logger, separator);
	this.andObject(object, prefix);
    }    
    
    @Override
    public LogDataConjunction value(Object value) {
	keyValueMap.put(currentKey, value.toString());
	currentKey = null;
	return this;
    }

    @Override
    public LoggerDataBuilder and(String key) {
	this.currentKey = key;
	return this;
    }

    @Override
    public LogDataConjunction andObject(Object object) {
	String qualifiedName = object.getClass().getName();
	String prefix = qualifiedName.substring(qualifiedName.lastIndexOf(".")+1);
	
	return this.andObject(object, prefix.replaceFirst(new Character(prefix.charAt(0)).toString(), 
		new Character(prefix.charAt(0)).toString().toLowerCase()));
    }

    @Override
    public LogDataConjunction andObject(Object object, String prefix) {
	this.objects.put(prefix, object);
	return this;
    }

    protected String getFullMessage(String message) {
	if (!hasSerializableData())
	    return message;
	
	StringBuilder builder = new StringBuilder(message.trim());
	
	
	if (hasSerializableObjects()) {
	    builder.append(" ");
	    builder.append(serialize(objects));
	    objects.clear();
	}
	
	if (hasSerializableMap()) {
	    builder.append(" ");
	    builder.append(serializeMap(keyValueMap));
	    keyValueMap.clear();
	}
	
	builder.append(" ");
	builder.append(getLogLevelAttribute());	
	
	System.out.println(builder);
	return builder.toString();
    }

    private String getLogLevelAttribute() {
	return "logLevel" + this.separator + this.level.toString();
    }

    private boolean hasSerializableData() {
	return hasSerializableMap() || hasSerializableObjects();
    }

    private boolean hasSerializableObjects() {
	return objects != null && objects.size() > 0;
    }

    private boolean hasSerializableMap() {
	return keyValueMap != null && keyValueMap.size() > 0;
    }

    @SuppressWarnings("rawtypes")
    private String serialize(Map<String, Object> objects) {
	StringBuilder builder = new StringBuilder();

	if (objects != null && objects.size() > 0) {
	    for (Entry<String, Object> entry : objects.entrySet()) {
		if (entry.getValue() instanceof Map) {
		    builder.append(this.serializeMap((Map) entry.getValue()));
		} else {
		    builder.append(this.serializeObject(entry));
		}
	    }
	}
	return builder.toString();
    }

    private String serializeObject(Entry<String, Object> entry) {
	
	StringBuilder builder = new StringBuilder();

	Object object = entry.getValue();
	String prefix = entry.getKey();
	
	Mirror mirror = new Mirror();
	
	List<Field> fields = mirror.on(object.getClass()).reflectAll().fields();
	
	if (fields != null && fields.size() > 0) {
	    for (Field field : fields) {
		Object value = mirror.on(object).get().field(field.getName());
		if (value != null) {
		    builder.append(formatKeyValue(prefix + "." + field.getName(), value.toString()));
		}
	    }
	}
	
	return builder.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String serializeMap(Map map) {
	
	StringBuilder builder = new StringBuilder();
	
	if (map != null && map.size() > 0) {
	    for (Entry entry : (Set<Entry>) map.entrySet()) {
		if (entry.getValue() != null) {
		    builder.append(formatKeyValue(entry.getKey(), entry.getValue()));
		}
	    }
	}
	
	return builder.toString();
    }
    
    private String formatKeyValue(Object key, Object value) {
	
	StringBuilder builder = new StringBuilder();
	builder.append(" ");
	builder.append(key.toString());
	builder.append(this.separator);
	builder.append(value.toString());
	builder.append(" ");
	return builder.toString();
    }
    

}
