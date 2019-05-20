/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
 */
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.utils.MapData;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.option.OptionValue;
import com.dtolabs.rundeck.plugins.user.groups.UserGroupSourcePlugin;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptUserGroupSource extends BaseScriptPlugin implements UserGroupSourcePlugin {
    private static final Logger                LOG          = Logger.getLogger(ScriptOptionValues.class);
    private static final String                START_MARKER = "==START_GROUPS==";
    private static final String                END_MARKER   = "==END_GROUPS==";
    private final        ServiceProviderLoader pluginManager;
    public ScriptUserGroupSource(final ScriptPluginProvider provider, final ServiceProviderLoader pluginManager) {
        super(provider);
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean isAllowCustomProperties() {
        return true;
    }

    @Override
    public List<String> getGroups(final String username, Map<String,Object> config) {
        DataContext ctx = createScriptDataContext(DataContextUtils.context("config", MapData.toStringStringMap(config)).getData());
        final ExecArgList scriptArgsList = createScriptArgsList(ctx);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        List<String> groups = new ArrayList<>();
        int result = -1;
        try {
            result = ScriptExecUtil.runLocalCommand(System.getProperty("os.name").toLowerCase(),
                                                    scriptArgsList,
                                                    ctx,
                                                    null,
                                                    outStream,
                                                    errStream
            );
            if(result == 0) {
                try(
                        BufferedReader
                                reader =
                                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outStream.toByteArray())))) {

                    boolean started = false, ended = false;
                    String line = null;
                    while((line = reader.readLine()) != null){
                        if (line.startsWith("#")) continue;
                        if (line.equals(START_MARKER)) {
                            started = true;
                            continue;
                        }
                        if (line.equals(END_MARKER)) {
                            ended = true;
                            continue;
                        }
                        if(started && !ended) {
                            groups.add(line);
                        }
                    }
                }
            } else {
                LOG.error(new String(errStream.toByteArray()));
            }

        } catch(Exception ex) {
            LOG.error(ServiceNameConstants.UserGroupSource + " plugin: " + getProvider().getName() + " failed", ex);
        }
        return groups;
    }
}
