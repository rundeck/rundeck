package org.rundeck.core.execution;

public interface BaseCommandExec {
    default String getArgString() {return null;};
    default String getAdhocRemoteString(){return null;};
    default String getAdhocLocalString(){return null;};
    default String getAdhocFilepath(){return null;};
    default Boolean getAdhocExecution(){return null;};
    default String getScriptInterpreter(){return null;};
    default String getFileExtension(){return null;};
    default Boolean getInterpreterArgsQuoted(){return null;};
    default Boolean getExpandTokenInScriptFile(){return null;};
}
