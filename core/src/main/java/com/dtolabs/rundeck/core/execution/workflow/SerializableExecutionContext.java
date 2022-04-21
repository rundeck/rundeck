package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.INodeEntry;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class SerializableExecutionContext {
    private String oid = UUID.randomUUID().toString();
    private String pluginService;
    private String plugin;
    private String project;
    private Integer logLevel;
    private INodeEntry node;
    private Map<String, Object> instanceConfiguration;
    private Map<String, Map<String,String>> privateContext;
    private Map<String, Map<String,String>> dataContext;
    private Map<String, Map<String,String>> nodeContext;
    private Map<String,String> projectProperties;

}
