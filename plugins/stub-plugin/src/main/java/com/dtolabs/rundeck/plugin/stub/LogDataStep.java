/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * @author greg
 * @since 5/24/17
 */

@Plugin(service = ServiceNameConstants.WorkflowStep, name = LogDataStep.PROVIDER_NAME)
@PluginDescription(title = "Log Data Step", description = "Log all context data values")
public class LogDataStep implements StepPlugin {
    public static final String PROVIDER_NAME = "log-data-step";

    @PluginProperty(title = "Debug Only", description = "Only log data at Debug level", defaultValue = "false")
    boolean debugOnly;


    @Override
    public void executeStep(
            final PluginStepContext context, final Map<String, Object> configuration
    ) throws StepException
    {
        MultiDataContext<ContextView, DataContext> sharedDataContext = context.getExecutionContext()
                                                                              .getSharedDataContext()
                                                                              .consolidate();
        ObjectMapper objectMapper = new ObjectMapper();
        Set<ContextView> keys = new TreeSet<>(sharedDataContext.getKeys());
        for (ContextView view : keys) {
            DataContext data = sharedDataContext.getData(view);
            Map<String, Map<String, String>> mapdata = data.getData();


            StringWriter stringWriter = new StringWriter();
            try {
                objectMapper.writeValue(stringWriter, mapdata);

                HashMap<String, String> meta = new HashMap<>();
                meta.put("content-data-type", "application/json");
                meta.put("content-meta:table-title", viewString(view));
                context.getExecutionContext().getExecutionListener().log(
                        debugOnly ? 3 : 2,
                        stringWriter.toString(),
                        meta
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public String viewString(final ContextView view) {
        if (view.isGlobal()) {
            return "global";
        }
        if (null != view.getNodeName()) {
            return (view.getStepNumber() != null ? view.getStepNumber() : "") + "@" + view.getNodeName();
        }
        if (null != view.getStepNumber()) {
            return view.getStepNumber().toString();
        }
        return view.toString();
    }

}
