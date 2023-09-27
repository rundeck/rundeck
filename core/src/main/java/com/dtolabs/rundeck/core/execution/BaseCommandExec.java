package com.dtolabs.rundeck.core.execution;

public interface BaseCommandExec {
    String getArgString();
    String getAdhocRemoteString();
    String getAdhocLocalString();
    String getAdhocFilepath();
    Boolean getAdhocExecution();
    String getScriptInterpreter();
    String getFileExtension();
    Boolean getInterpreterArgsQuoted();
    Boolean getExpandTokenInScriptFile();
}
