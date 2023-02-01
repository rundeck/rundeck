package org.rundeck.app.data.execution

import org.rundeck.app.execution.ExecutionCreationSource

class DefaultExecutionCreationSource implements ExecutionCreationSource {
    def props = [:]
    @Override
    def <T> T get(String key, Class<T> type) {
        return (T)props[key]
    }

    @Override
    void put(String key, Object obj) {
        props[key] = obj
    }
}
