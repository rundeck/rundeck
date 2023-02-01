package org.rundeck.app.execution;

public interface ExecutionCreator<E> {
    E createExecution(ExecutionCreationSource sourceData);

}
