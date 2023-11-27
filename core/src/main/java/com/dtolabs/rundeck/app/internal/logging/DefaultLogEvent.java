/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 */
package com.dtolabs.rundeck.app.internal.logging;

import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DefaultLogEvent implements LogEvent {
    private LogLevel           loglevel;
    private Date               datetime;
    private String             message;
    private String             eventType;
    private Map<String,String> metadata;

    public DefaultLogEvent(){}

    public DefaultLogEvent(
            final LogLevel loglevel,
            final Date datetime,
            final String message,
            final String eventType,
            final Map<String, String> metadata
    ) {
        this.loglevel = loglevel;
        this.datetime = datetime;
        this.message = message;
        this.eventType = eventType;
        this.metadata = metadata;
    }

    public DefaultLogEvent(LogEvent event){
        this(event,new HashMap<String,String>());
    }

    public DefaultLogEvent(LogEvent event, Map<String,String> defaultMetadata){
        this.loglevel = event.getLoglevel();
        this.datetime = event.getDatetime();
        this.message = event.getMessage();
        this.eventType = event.getEventType();
        defaultMetadata.putAll(event.getMetadata() != null ? event.getMetadata() : new HashMap<String,String>());
        this.metadata=defaultMetadata;
    }

    static DefaultLogEvent with(LogEvent event, Map<String,String> metadata) {
        return new DefaultLogEvent(event, metadata);
    }
    @Override
    public String toString() {
        return "DefaultLogEvent{" +
               ", eventType=" + eventType +
               ", loglevel=" + loglevel +
               ", datetime=" + datetime +
               ", message='" + message + '\'' +
               ", metadata=" + metadata +
               '}';
    }

    @Override
    public LogLevel getLoglevel() {
        return loglevel;
    }

    @Override
    public Date getDatetime() {
        return datetime;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setMetadata(final Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }
}
