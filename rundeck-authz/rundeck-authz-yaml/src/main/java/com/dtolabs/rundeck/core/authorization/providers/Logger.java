package com.dtolabs.rundeck.core.authorization.providers;

public interface Logger {
    void warn(String log);
    void error(String log);
    void error(String log, Throwable e);
    void debug(String log);
    void debug(String log, Throwable e);
}
