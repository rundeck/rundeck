package com.dtolabs.rundeck.core.logging;

import java.util.Date;
import java.util.Map;

/** $INTERFACE is ... User: greg Date: 5/23/13 Time: 2:38 PM */
class LogEventImpl implements LogEvent {
    private String eventType;
    private Date datetime;
    private LogLevel loglevel;
    private String message;
    private Map<String, String> metadata;

    private LogEventImpl(String eventType, Date datetime, LogLevel loglevel, String message, Map<String,
            String> metadata) {
        this.eventType = eventType;
        this.datetime = datetime;
        this.loglevel = loglevel;
        this.message = message;
        this.metadata = metadata;
    }

    public static LogEventImpl create(String eventType, Date datetime, LogLevel logLevel, String message, Map<String,
            String> metadata) {
        return new LogEventImpl(eventType, datetime, logLevel, message, metadata);
    }

    public String getEventType() {
        return eventType;
    }

    public Date getDatetime() {
        return datetime;
    }

    public LogLevel getLoglevel() {
        return loglevel;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
