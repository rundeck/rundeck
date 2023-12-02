package org.rundeck.core.execution;

import java.util.Map;

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

    default Map convertToPluginConfig(){
        Map<String, Object> pluginConfig = new java.util.HashMap<>();
        pluginConfig.put("argString", getArgString());
        pluginConfig.put("adhocRemoteString", getAdhocRemoteString());
        pluginConfig.put("adhocLocalString", getAdhocLocalString());
        pluginConfig.put("adhocFilepath", getAdhocFilepath());
        pluginConfig.put("adhocExecution", getAdhocExecution());
        pluginConfig.put("scriptInterpreter", getScriptInterpreter());
        pluginConfig.put("fileExtension", getFileExtension());
        pluginConfig.put("interpreterArgsQuoted", getInterpreterArgsQuoted());
        pluginConfig.put("expandTokenInScriptFile", getExpandTokenInScriptFile());
        return pluginConfig;
    }
}
