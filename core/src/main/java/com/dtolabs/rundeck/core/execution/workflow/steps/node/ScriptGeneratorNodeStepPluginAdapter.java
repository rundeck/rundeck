/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
* ScriptGeneratorNodeStepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 6:09 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.step.GeneratedScript;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;
import com.dtolabs.rundeck.plugins.step.ScriptGeneratorNodeStepPlugin;

import java.io.File;
import java.util.*;


/**
 * ScriptGeneratorNodeStepPluginAdapter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptGeneratorNodeStepPluginAdapter implements NodeStepExecutor, Describable {
    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            final Describable desc = (Describable) plugin;
            return desc.getDescription();
        }
        return null;
    }

    private ScriptGeneratorNodeStepPlugin plugin;

    public ScriptGeneratorNodeStepPluginAdapter(final ScriptGeneratorNodeStepPlugin plugin) {
        this.plugin = plugin;
    }

    static class Convert implements Converter<ScriptGeneratorNodeStepPlugin, NodeStepExecutor> {
        @Override
        public NodeStepExecutor convert(final ScriptGeneratorNodeStepPlugin plugin) {
            return new ScriptGeneratorNodeStepPluginAdapter(plugin);
        }
    }

    public static final Convert CONVERTER = new Convert();

    @Override
    public NodeStepResult executeNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node)
        throws NodeStepException {
        PluginStepItem item1 = NodeStepPluginAdapter.toPluginStepItem(item, context);
        GeneratedScript script = plugin.generateScript(context, item1, node);
        ExecutionService executionService = context.getFramework().getExecutionService();
        if (null != script.getCommand()) {
            //execute the command
            try {
                return executionService.executeCommand(context, script.getCommand(), node);
            } catch (ExecutionException e) {
                throw new NodeStepException(e, node.getNodename());
            }
        }else if (null != script.getScript()) {
            final String filepath; //result file path
            try {
                filepath = executionService.fileCopyScriptContent(context, script.getScript(), node);
            } catch (FileCopierException e) {
                throw new NodeStepException(e, node.getNodename());
            }
            return ScriptFileNodeStepExecutor.executeRemoteScript(context,
                                                           context.getFramework(),
                                                           node,
                                                           script.getArgs(),
                                                           filepath);

        }else{
            return new NodeStepResultImpl(false, node);
        }
    }
}
