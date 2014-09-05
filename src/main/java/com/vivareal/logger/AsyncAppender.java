/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Contibutors:  Aaron Greenhouse <aarong@cs.cmu.edu>
//               Thomas Tuft Muller <ttm@online.no>
package com.vivareal.logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

/**
 * The AsyncAppender lets users log events asynchronously.
 * <p/>
 * <p/>
 * The AsyncAppender will collect the events sent to it and then dispatch them
 * to all the appenders that are attached to it. You can attach multiple
 * appenders to an AsyncAppender.
 * </p>
 * <p/>
 * <p/>
 * The AsyncAppender uses a separate thread to serve the events in its buffer.
 * </p>
 * <p/>
 * <b>Important note:</b> The <code>AsyncAppender</code> can only be script
 * configured using the {@link org.apache.log4j.xml.DOMConfigurator}.
 * </p>
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Curt Arnold
 * @since 0.9.1
 */
public class AsyncAppender extends AppenderSkeleton implements AppenderAttachable {
    /**
     * The default buffer size is set to 128 events.
     */
    public static final int DEFAULT_BUFFER_SIZE = 128;

    /**
     * Event buffer, also used as monitor to protect itself and discardMap from
     * simulatenous modifications.
     */
    private final List buffer = new ArrayList();

    /**
     * Map of DiscardSummary objects keyed by logger name.
     */
    private final Map discardMap = new HashMap();

    /**
     * Buffer size.
     */
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    /** Nested appenders. */
    CustomAppenderAttachable aai;

    /**
     * Nested appenders.
     */
    private final CustomAppenderAttachable appenders;

    /**
     * Dispatcher.
     */
    private Thread dispatcher;

    /**
     * Should location info be included in dispatched messages.
     */
    private boolean locationInfo = false;

    /**
     * Does appender block when buffer is full.
     * This will always be false, because we are ASYNC
     */
    private boolean blocking = false;

    /**
     * Create new instance.
     */
    public AsyncAppender() {
	// using CustomAppender to deal with runtime exception that can happen
	appenders = new CustomAppenderAttachable();
	aai = appenders;

	setupDispatcher();
    }

    private void setupDispatcher() {
	dispatcher = new Thread(new Dispatcher(this, buffer, discardMap,
		appenders));

	dispatcher.setDaemon(true);

	dispatcher.setName("AsyncAppender-Dispatcher-" + dispatcher.getName());
	dispatcher.start();
    }

    /**
     * Add appender.
     *
     * @param newAppender
     *            appender to add, may not be null.
     */
    public void addAppender(final Appender newAppender) {
	synchronized (appenders) {
	    appenders.addAppender(newAppender);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void append(final LoggingEvent event) {
	// if dispatcher thread has died then setup it again to remain async
	// See bug 23021
	if ((dispatcher == null) || !dispatcher.isAlive() || (bufferSize <= 0)) {
	    setupDispatcher();
	}

	event.getNDC();
	event.getThreadName();
	event.getMDCCopy();
	if (locationInfo) {
	    event.getLocationInformation();
	}
	event.getRenderedMessage();
	event.getThrowableStrRep();

	synchronized (buffer) {
	    while (true) {
		int previousSize = buffer.size();

		if (previousSize < bufferSize) {
		    buffer.add(event);

		    if (previousSize == 0) {
			buffer.notifyAll();
		    }

		    break;
		}

		boolean discard = true;
		if (blocking && !Thread.interrupted()
			&& Thread.currentThread() != dispatcher) {
		    try {
			buffer.wait();
			discard = false;
		    } catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		    }
		}

		if (discard) {
		    String loggerName = event.getLoggerName();
		    DiscardSummary summary = (DiscardSummary) discardMap
			    .get(loggerName);

		    if (summary == null) {
			summary = new DiscardSummary(event);
			discardMap.put(loggerName, summary);
		    } else {
			summary.add(event);
		    }

		    break;
		}
	    }
	}
    }

    /**
     * Close this <code>AsyncAppender</code> by interrupting the dispatcher
     * thread which will process all pending events before exiting.
     */
    public void close() {
	/**
	 * Set closed flag and notify all threads to check their conditions.
	 * Should result in dispatcher terminating.
	 */
	synchronized (buffer) {
	    closed = true;
	    buffer.notifyAll();
	}

	try {
	    dispatcher.join();
	} catch (InterruptedException e) {
	    Thread.currentThread().interrupt();
	    org.apache.log4j.helpers.LogLog.error(
		    "Got an InterruptedException while waiting for the "
			    + "dispatcher to finish.", e);
	}

	synchronized (appenders) {
	    Enumeration iter = appenders.getAllAppenders();

	    if (iter != null) {
		while (iter.hasMoreElements()) {
		    Object next = iter.nextElement();

		    if (next instanceof Appender) {
			((Appender) next).close();
		    }
		}
	    }
	}
    }

    /**
     * Get iterator over attached appenders.
     * 
     * @return iterator or null if no attached appenders.
     */
    public Enumeration getAllAppenders() {
	synchronized (appenders) {
	    return appenders.getAllAppenders();
	}
    }

    /**
     * Get appender by name.
     *
     * @param name
     *            name, may not be null.
     * @return matching appender or null.
     */
    public Appender getAppender(final String name) {
	synchronized (appenders) {
	    return appenders.getAppender(name);
	}
    }

    /**
     * Gets whether the location of the logging request call should be captured.
     *
     * @return the current value of the <b>LocationInfo</b> option.
     */
    public boolean getLocationInfo() {
	return locationInfo;
    }

    /**
     * Determines if specified appender is attached.
     * 
     * @param appender
     *            appender.
     * @return true if attached.
     */
    public boolean isAttached(final Appender appender) {
	synchronized (appenders) {
	    return appenders.isAttached(appender);
	}
    }

    /**
     * {@inheritDoc}
     */
    public boolean requiresLayout() {
	return false;
    }

    /**
     * Removes and closes all attached appenders.
     */
    public void removeAllAppenders() {
	synchronized (appenders) {
	    appenders.removeAllAppenders();
	}
    }

    /**
     * Removes an appender.
     * 
     * @param appender
     *            appender to remove.
     */
    public void removeAppender(final Appender appender) {
	synchronized (appenders) {
	    appenders.removeAppender(appender);
	}
    }

    /**
     * Remove appender by name.
     * 
     * @param name
     *            name.
     */
    public void removeAppender(final String name) {
	synchronized (appenders) {
	    appenders.removeAppender(name);
	}
    }

    /**
     * The <b>LocationInfo</b> option takes a boolean value. By default, it is
     * set to false which means there will be no effort to extract the location
     * information related to the event. As a result, the event that will be
     * ultimately logged will likely to contain the wrong location information
     * (if present in the log format).
     * <p/>
     * <p/>
     * Location information extraction is comparatively very slow and should be
     * avoided unless performance is not a concern.
     * </p>
     * 
     * @param flag
     *            true if location information should be extracted.
     */
    public void setLocationInfo(final boolean flag) {
	locationInfo = flag;
    }

    /**
     * Sets the number of messages allowed in the event buffer before the
     * calling thread is blocked (if blocking is true) or until messages are
     * summarized and discarded. Changing the size will not affect messages
     * already in the buffer.
     *
     * @param size
     *            buffer size, must be positive.
     */
    public void setBufferSize(final int size) {
	//
	// log4j 1.2 would throw exception if size was negative
	// and deadlock if size was zero.
	//
	if (size < 0) {
	    throw new java.lang.NegativeArraySizeException("size");
	}

	synchronized (buffer) {
	    //
	    // don't let size be zero.
	    //
	    bufferSize = (size < 1) ? 1 : size;
	    buffer.notifyAll();
	}
    }

    /**
     * Gets the current buffer size.
     * 
     * @return the current value of the <b>BufferSize</b> option.
     */
    public int getBufferSize() {
	return bufferSize;
    }

    /**
     * Summary of discarded logging events for a logger.
     */
    private static final class DiscardSummary {
	/**
	 * First event of the highest severity.
	 */
	private LoggingEvent maxEvent;

	/**
	 * Total count of messages discarded.
	 */
	private int count;

	/**
	 * Create new instance.
	 *
	 * @param event
	 *            event, may not be null.
	 */
	public DiscardSummary(final LoggingEvent event) {
	    maxEvent = event;
	    count = 1;
	}

	/**
	 * Add discarded event to summary.
	 *
	 * @param event
	 *            event, may not be null.
	 */
	public void add(final LoggingEvent event) {
	    if (event.getLevel().toInt() > maxEvent.getLevel().toInt()) {
		maxEvent = event;
	    }

	    count++;
	}

	/**
	 * Create event with summary information.
	 *
	 * @return new event.
	 */
	public LoggingEvent createEvent() {
	    String msg = MessageFormat
		    .format("Discarded {0} messages due to full event buffer including: {1}",
			    new Object[] { new Integer(count),
				    maxEvent.getMessage() });

	    return new LoggingEvent(
		    "org.apache.log4j.AsyncAppender.DONT_REPORT_LOCATION",
		    Logger.getLogger(maxEvent.getLoggerName()),
		    maxEvent.getLevel(), msg, null);
	}
    }

    /**
     * Event dispatcher.
     */
    private static class Dispatcher implements Runnable {
	/**
	 * Parent AsyncAppender.
	 */
	private final AsyncAppender parent;

	/**
	 * Event buffer.
	 */
	private final List buffer;

	/**
	 * Map of DiscardSummary keyed by logger name.
	 */
	private final Map discardMap;

	/**
	 * Wrapped appenders.
	 */
	private final CustomAppenderAttachable appenders;

	/**
	 * Create new instance of dispatcher.
	 *
	 * @param parent
	 *            parent AsyncAppender, may not be null.
	 * @param buffer
	 *            event buffer, may not be null.
	 * @param discardMap
	 *            discard map, may not be null.
	 * @param appenders
	 *            appenders, may not be null.
	 */
	public Dispatcher(final AsyncAppender parent, final List buffer,
		final Map discardMap, final CustomAppenderAttachable appenders) {

	    this.parent = parent;
	    this.buffer = buffer;
	    this.appenders = appenders;
	    this.discardMap = discardMap;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
	    boolean isActive = true;

	    try {
		while (isActive) {
		    LoggingEvent[] events = null;

		    synchronized (buffer) {
			int bufferSize = buffer.size();
			isActive = !parent.closed;

			while ((bufferSize == 0) && isActive) {
			    buffer.wait();
			    bufferSize = buffer.size();
			    isActive = !parent.closed;
			}

			if (bufferSize > 0) {
			    events = new LoggingEvent[bufferSize
				    + discardMap.size()];
			    buffer.toArray(events);

			    int index = bufferSize;

			    for (Iterator iter = discardMap.values().iterator(); iter
				    .hasNext();) {
				events[index++] = ((DiscardSummary) iter.next())
					.createEvent();
			    }

			    buffer.clear();
			    discardMap.clear();

			    buffer.notifyAll();
			}
		    }

		    if (events != null) {
			for (int i = 0; i < events.length; i++) {
			    synchronized (appenders) {
				appenders.appendLoopOnAppenders(events[i]);
			    }
			}
		    }
		}
	    } catch (InterruptedException ex) {
		Thread.currentThread().interrupt();
	    }
	}
    }
}
