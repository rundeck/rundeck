package com.dtolabs.rundeck.core.rules;

/**
 * Created by greg on 5/18/16.
 */
public enum WorkflowSystemEventType {
    WillProcessStateChange,
    DidProcessStateChange,
    OperationSuccess,
    OperationFailed,
    WillSkipOperation,
    LoopProgress,
    Interrupted,
    IncompleteOperations,
    Complete,
    Begin,
    EndOfChanges, WillRunOperation, WillShutdown, WorkflowEndState,
}
