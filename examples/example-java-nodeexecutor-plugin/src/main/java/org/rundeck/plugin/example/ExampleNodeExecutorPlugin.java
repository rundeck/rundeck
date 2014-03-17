package org.rundeck.plugin.example;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

/**
 * ExampleNodeExecutorPlugin is an example {@link NodeExecutor} plugin implementation.
 *
 * @author greg
 * @since 2014-03-17
 */
@Plugin(name = ExampleNodeExecutorPlugin.PROVIDER_NAME, service = ServiceNameConstants.NodeExecutor)
@PluginDescription(title = "Java Example",description = "Example NodeExecutor written in Java")
public class ExampleNodeExecutorPlugin implements NodeExecutor {

    public static final String PROVIDER_NAME = "example-nodeexecutor";

    public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) {
        System.out.println("Example node executor for node " + node.getNodename());
        String foo = node.getAttributes().get("foo");
        String bar = node.getAttributes().get("bar");
        System.out.println("Using foo value: " + foo);
        context.getExecutionListener().log(0, "Error level logging, this is bar: " + bar);
        return NodeExecutorResultImpl.createSuccess(node);
    }
}
