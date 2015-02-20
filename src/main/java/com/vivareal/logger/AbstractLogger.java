package com.vivareal.logger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public abstract class AbstractLogger implements BaseLogger {

    protected final Logger logger;
    protected Level level;
    
    protected AbstractLogger(Logger logger) {
	this.logger = logger;
    }

    @Override
    public void error(String message) {
	level = Level.ERROR;
	this.logger.error(getFullMessage(message));
    }

    @Override
    public void error(String message, Throwable t) {
	level = Level.ERROR;
	this.logger.error(getFullMessage(message), t);
    }

    @Override
    public void fatal(String message) {
	level = Level.FATAL;
	this.logger.fatal(getFullMessage(message));
    }

    @Override
    public void fatal(String message, Throwable t) {
	level = Level.FATAL;
	this.logger.fatal(getFullMessage(message), t);
    }

    @Override
    public void warn(String message) {
	level = Level.WARN;
	this.logger.warn(getFullMessage(message));
    }

    @Override
    public void warn(String message, Throwable t) {
	level = Level.WARN;
	this.logger.warn(getFullMessage(message), t);
    }

    @Override
    public void info(String message) {
	level = Level.INFO;
	this.logger.info(getFullMessage(message));
    }

    @Override
    public void info(String message, Throwable t) {
	level = Level.INFO;
	this.logger.info(getFullMessage(message), t);
    }

    @Override
    public void debug(String message) {
	level = Level.DEBUG;
	this.logger.debug(getFullMessage(message));
    }

    @Override
    public void debug(String message, Throwable t) {
	level = Level.DEBUG;
	this.logger.debug(getFullMessage(message), t);
    }

    @Override
    public void trace(String message) {
	level = Level.TRACE;
	this.logger.trace(message);
    }

    @Override
    public void trace(String message, Throwable t) {
	level = Level.TRACE;
	this.logger.trace(message, t);
    }

    protected abstract String getFullMessage(String message);
}
