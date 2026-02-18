package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeExecutorUtils {

    static final String RD_VARIABLE_PATTERN = "ssh-variable-export-pattern";
    static final String RD_VARIABLE_PATTERN_SEPARATOR = "ssh-variable-export-separator";
    static final String RD_VARIABLE_PATTERN_EXCLUDE_NODES = "ssh-variable-export-exclude-nodes";
    static final String RD_VARIABLE_PATTERN_EXCLUDE_NODES_PATTERN = "RD_NODE";
    static final String RD_VARIABLE_PATTERN_DEFAULT_SEPARATOR = ";";

    /**
     * It checkf if the node allows RD variable inyection, if so, it will be added as part of the execution command
     * @param node
     * @param nodeContext
     * @param commandList current command list to execute
     * @return String[] commands with exported variables
     */
    public static String[] getExportedVariablesForNode(final INodeEntry node,
                                                final ExecutionContext nodeContext,
                                                final List commandList){
        String exportedVariables = "";
        List<String> commandsList = new ArrayList<>();
        if(node.getAttributes().get(RD_VARIABLE_PATTERN) != null){
            List<String> envArgs = new ArrayList<>();
            String replacementPattern = node.getAttributes().get(RD_VARIABLE_PATTERN);
            final Map<String, String> envVars = DataContextUtils.generateEnvVarsFromContext(nodeContext.getDataContext());
            envVars.forEach((k,v)->{
                if(node.getAttributes().get(RD_VARIABLE_PATTERN_EXCLUDE_NODES) != null
                        && Boolean.TRUE.equals(Boolean.valueOf(node.getAttributes().get(RD_VARIABLE_PATTERN_EXCLUDE_NODES)))){
                    if(!k.startsWith(RD_VARIABLE_PATTERN_EXCLUDE_NODES_PATTERN)){
                        envArgs.add(replacementPattern.replace("{key}", k).replace("{value}", v));
                    }
                }else{
                    envArgs.add(replacementPattern.replace("{key}", k).replace("{value}", v));
                }
            });
            if(!envArgs.isEmpty()){
                String variablePatternSeparator = RD_VARIABLE_PATTERN_DEFAULT_SEPARATOR;
                if(node.getAttributes().get(RD_VARIABLE_PATTERN_SEPARATOR) != null){
                    variablePatternSeparator = node.getAttributes().get(RD_VARIABLE_PATTERN_SEPARATOR);
                }
                exportedVariables = StringUtils.join(envArgs, StringEscapeUtils.unescapeJava(variablePatternSeparator)) + StringEscapeUtils.unescapeJava(variablePatternSeparator);
                commandsList.add(exportedVariables);
            }
        }
        commandsList.addAll(commandList);
        return commandsList.toArray(new String[commandsList.size()]);
    }
}
