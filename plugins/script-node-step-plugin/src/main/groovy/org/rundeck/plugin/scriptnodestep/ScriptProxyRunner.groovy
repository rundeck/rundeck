package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.proxy.ProxyRunnerPlugin
import com.dtolabs.rundeck.core.execution.proxy.ProxySecretBundleCreator
import groovy.transform.CompileStatic;

import java.util.ArrayList
import java.util.List
import java.util.Map

@CompileStatic
class ScriptProxyRunner implements ProxyRunnerPlugin {

    @Override
    Map<String, String> getRuntimeProperties(ExecutionContext context) {
        return context.getFramework().getFrameworkProjectMgr().loadProjectConfig(context.frameworkProject).getProjectProperties()
    }

    @Override
    Map<String, String> getRuntimeFrameworkProperties(ExecutionContext context){
        return context.getIFramework().getPropertyLookup().getPropertiesMap()
    }

    @Override
        //get shared secrets from the original node-executor plugin
    List<String> listSecretsPathWorkflowNodeStep(ExecutionContext context, INodeEntry node, Map<String, Object> configuration) {

        def executionService = context.getFramework().getNodeExecutorService()
        //get original node executor from node or project
        String orig = executionService.getDefaultProviderNameForNodeAndProject(node, context.getFrameworkProject())
        if (null != node.getAttributes() && null != node.getAttributes().get(executionService.getServiceProviderNodeAttributeForNode(
                node))) {
            orig = node.getAttributes().get(executionService.getServiceProviderNodeAttributeForNode(node));
        }
        //get provider
        def provider = executionService.providerOfType(orig)

        def list = new ArrayList<String>()
        if(provider instanceof ProxyRunnerPlugin){
            //get list of secrets from original node-executor plugin
            list = provider.listSecretsPath(context, node)
        }

        if(provider instanceof ProxySecretBundleCreator){
            //get list of secrets from original node-executor plugin
            list = provider.listSecretsPath(context, node)
        }

        return list
    }

}
