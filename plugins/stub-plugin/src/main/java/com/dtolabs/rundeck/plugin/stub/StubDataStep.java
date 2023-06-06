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

package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ExecutionEnvironmentConstants;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.*;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.CODE_SYNTAX_MODE;
import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DISPLAY_TYPE_KEY;

/**
 * @author greg
 * @since 5/2/17
 */

@Plugin(service = ServiceNameConstants.WorkflowStep, name = StubDataStep.PROVIDER_NAME)
@PluginDescription(title = "Data Step", description = "Produce data values")
@PluginMetadata(key = "faicon", value = "database")
@PluginMetadata(key = ExecutionEnvironmentConstants.ENVIRONMENT_TYPE_KEY, value = ExecutionEnvironmentConstants.LOCAL_RUNNER)
public class StubDataStep implements StepPlugin {
    public static final String PROVIDER_NAME = "stub-data-step";

    @PluginProperty(title = "Data",
            description = "Properties formatted data to set for the current node",
            required = true)
    @RenderingOptions(
            {
                    @RenderingOption(key = DISPLAY_TYPE_KEY, value = "CODE"),
                    @RenderingOption(key = CODE_SYNTAX_MODE, value = "properties"),
            }
    )
    private String data;

    @PluginProperty(title = "Format",
            description = "Format for the data",
            required = true,
            defaultValue = "properties")
    @SelectValues(values = {"properties", "json", "yaml"})
    private String format;

    public enum StubReason implements FailureReason {
        InvalidData
    }


    @Override
    public void executeStep(
            final PluginStepContext context, final Map<String, Object> configuration
    ) throws StepException
    {
        Properties props = parseData(format, data);

        addData(ContextView.global(), context, props, null);
        context.getLogger().log(2, String.format("Added %d data values", props.size()));

        if ("properties".equals(format)) {
            HashMap<String, String> meta = new HashMap<>();
            meta.put("content-data-type", "application/x-java-properties");
            context.getExecutionContext().getExecutionListener().event("log", data, meta);
        } else if ("json".equals(format)) {
            HashMap<String, String> meta = new HashMap<>();
            meta.put("content-data-type", "application/json");
            context.getExecutionContext().getExecutionListener().event("log", data, meta);
        }

    }

    public static void addData(
            final ContextView view,
            final PluginStepContext context,
            final Properties props,
            final Function<String, String> converter
    )
    {
        for (String name : props.stringPropertyNames()) {
            String property = props.getProperty(name);
            context.getOutputContext().addOutput(
                    view,
                    "stub",
                    name,
                    null != converter ? converter.apply(property) : property
            );
        }
    }

    public static Properties parseData(final String format, final String data) throws StepException {
        Properties props = new Properties();
        if ("properties".equals(format)) {
            try {
                props.load(new StringReader(data));
            } catch (IOException e) {
                throw new StepException(
                        "Could not read properties data",
                        e,
                        StubReason.InvalidData
                );
            }
        } else if ("json".equals(format)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, String> result = objectMapper.readValue(data, Map.class);
                props.putAll(result);
            } catch (IOException e) {
                throw new StepException(
                        "Could not read json data",
                        e,
                        StubReason.InvalidData
                );
            }

        } else if ("yaml".equals(format)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object ydata = yaml.load(data);
            if (ydata instanceof Map) {
                Map<String, String> result = (Map) ydata;
                props.putAll(result);
            } else {
                throw new StepException(
                        "Could not read yaml data",
                        StubReason.InvalidData
                );
            }

        }
        return props;
    }
}
