package com.dtolabs.rundeck.core.execution.proxy;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;

import java.util.List;
import java.util.Map;

public interface ProxyRunnerPlugin {


    default List<String> listSecretsPath(ExecutionContext context, INodeEntry node){
        return null;
    }

    default List<String> listSecretsPathWorkflowNodeStep(ExecutionContext context, INodeEntry node, Map<String, Object> configuration){
        return null;
    }

    default List<String> listSecretsPathWorkflowStep(ExecutionContext context, Map<String, Object> configuration){
        return null;
    }

    default List<String> listSecretsPathResourceModel(Map<String, Object> configuration){
        return null;
    }


    default Map<String, String> getRuntimeProperties(ExecutionContext context){
        return null;
    };

    default Map<String, String> getRuntimeFrameworkProperties(ExecutionContext context){
        return null;
    };
}
