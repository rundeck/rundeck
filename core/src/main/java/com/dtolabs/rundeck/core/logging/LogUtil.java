package com.dtolabs.rundeck.core.logging;

import java.util.Date;
import java.util.Map;

/**
 * Utility methods for logs
 */
public class LogUtil {

    public static final String EVENT_TYPE_LOG = "log";

    public static LogEvent logError(String message) {
        return logError(message, null);
    }

    public static LogEvent logError(String message, Map<String, String> metadata) {
        return log(message, metadata, LogLevel.ERROR);
    }

    public static LogEvent logNormal(String message) {
        return logNormal(message, null);
    }

    public static LogEvent logNormal(String message, Map<String, String> metadata) {
        return log(message, metadata, LogLevel.NORMAL);
    }

    public static LogEvent logWarn(String message) {
        return logWarn(message, null);
    }

    public static LogEvent logWarn(String message, Map<String, String> metadata) {
        return log(message, metadata, LogLevel.WARN);
    }

    public static LogEvent logVerbose(String message) {
        return logVerbose(message, null);
    }

    public static LogEvent logVerbose(String message, Map<String, String> metadata) {
        return log(message, metadata, LogLevel.VERBOSE);
    }

    public static LogEvent logDebug(String message) {
        return logDebug(message, null);
    }

    public static LogEvent logDebug(String message, Map<String, String> metadata) {
        return log(message, metadata, LogLevel.DEBUG);
    }

    private static LogEvent log(String message, Map<String, String> metadata, LogLevel level) {
        return event(EVENT_TYPE_LOG, level, message, metadata);
    }

    public static LogEvent event(String type, LogLevel level, String message, Map<String, String> metadata) {
        return LogEventImpl.create(type, new Date(), level, message, metadata);
    }
}
