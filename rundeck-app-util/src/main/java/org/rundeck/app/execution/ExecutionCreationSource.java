package org.rundeck.app.execution;

public interface ExecutionCreationSource {
    <T> T get(String key, Class<T> type);
    void put(String key, Object obj);
}
