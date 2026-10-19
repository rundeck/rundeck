/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.core.execution.workflow.OutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Shared logic for writing the resolved value of any property carrying {@code @PluginOutput} metadata
 * into a step's output context, so it can be referenced by conditional-logic steps later in the
 * workflow (as {@code "<Step Label> - <Property Name>"}, resolved to {@code ${N:group.name}}). Used by
 * both {@code StepPluginAdapter} (workflow steps) and {@code NodeStepPluginAdapter} (node steps) so the
 * capture logic doesn't need to be duplicated between the two.
 */
public final class PluginOutputCapture {

    private static final Logger log = LoggerFactory.getLogger(PluginOutputCapture.class.getName());

    private PluginOutputCapture() {
    }

    /**
     * Capture every {@code @PluginOutput}-annotated property's resolved value into the given output
     * context. Callers must only invoke this after the plugin's execution method returns successfully:
     * a property carrying only {@code @PluginOutput} (no {@code @PluginProperty}) is a computed value
     * the plugin sets on its own field *during* execution, so it doesn't exist beforehand, and a
     * failed/thrown execution should expose no output.
     *
     * @param outputContext  the step's output context (may be node-scoped already, for node steps)
     * @param description    the plugin's description, providing property output metadata
     * @param pluginInstance the plugin instance, for reflection fallback when a property has no entry
     *                       in {@code config}
     * @param config         the resolved/expanded instance configuration for this step, or null
     */
    public static void captureOutputMetadataValues(
            final OutputContext outputContext,
            final Description description,
            final Object pluginInstance,
            final Map<String, Object> config
    )
    {
        if (description == null || description.getProperties() == null || outputContext == null) {
            return;
        }
        for (final Property property : description.getProperties()) {
            final List<PluginOutputMetadata> outputMetadata = property.getOutputMetadata();
            if (outputMetadata == null || outputMetadata.isEmpty()) {
                continue;
            }
            try {
                final Object value = resolvePropertyValue(property, pluginInstance, config);
                if (value == null) {
                    continue;
                }
                for (final PluginOutputMetadata metadata : outputMetadata) {
                    outputContext.addOutput(metadata.getGroup(), metadata.getName(), value.toString());
                }
            } catch (Throwable e) {
                // Capture is best-effort metadata derived via reflection over a plugin-controlled
                // field (setAccessible can throw unchecked InaccessibleObjectException/
                // SecurityException, and value.toString() runs arbitrary plugin code) -- none of
                // that should be able to turn an already-successful step execution into a failure.
                log.debug("Failed to capture @PluginOutput value for property '" + property.getName() + "': " + e, e);
            }
        }
    }

    /**
     * Resolve a property's configured/computed value. Properties that map to an actual field on the
     * plugin instance are set directly on that field (and removed from the returned instance
     * configuration map) by {@code PluginAdapterUtility#configureProperties}, so the value is read
     * back off the plugin instance's field in that (common) case; otherwise it's read from the
     * leftover config map. A property carrying only {@code @PluginOutput} (no {@code @PluginProperty})
     * has no entry in either the config map or {@link PluginAdapterImpl#fieldForPropertyName} (which
     * only matches {@code @PluginProperty} fields), so as a final fallback its value is read directly
     * off the plugin's own field of the same name, which is where {@code PluginAdapterImpl} sourced
     * the property's name/title from when it built the Description for such a field.
     * <p>
     * An {@link Property#isOutputOnly()} property is never read from {@code config}, even if a job
     * defined via API/YAML happens to set a {@code configuration} entry matching its name: since the
     * property has no {@code @PluginProperty}, that entry can never be a legitimate configured value,
     * and reading it would silently shadow the value the plugin actually computed during execution.
     */
    private static Object resolvePropertyValue(
            final Property property,
            final Object pluginInstance,
            final Map<String, Object> config
    ) throws IllegalAccessException
    {
        if (!property.isOutputOnly() && config != null && config.containsKey(property.getName())) {
            return config.get(property.getName());
        }
        if (pluginInstance == null) {
            return null;
        }
        Field field = new PluginAdapterImpl().fieldForPropertyName(property.getName(), pluginInstance);
        if (field == null) {
            field = fieldByName(pluginInstance.getClass(), property.getName());
        }
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        return field.get(pluginInstance);
    }

    /**
     * Find a declared field by exact name, searching up the class hierarchy.
     */
    private static Field fieldByName(final Class<?> type, final String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // try the superclass
            }
        }
        return null;
    }
}
