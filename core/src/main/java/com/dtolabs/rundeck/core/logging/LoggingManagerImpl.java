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

package com.dtolabs.rundeck.core.logging;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.HasLoggingFilterConfiguration;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.plugins.PluginConfiguration;
import com.dtolabs.rundeck.core.plugins.SimplePluginProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Creates plugin logging managers that can override the thread's log sink
 *
 * @author greg
 * @since 5/11/17
 */
public class LoggingManagerImpl implements LoggingManager {
    private final OverridableStreamingLogWriter writer;
    private final ExecutionLogger directLogger;
    private final SimplePluginProviderLoader<LogFilterPlugin> pluginLoader;
    private List<PluginConfiguration> globalPluginConfigs;
    LinkedBlockingQueue<MyPluginLoggingManager> logging = new LinkedBlockingQueue<>();


    /**
     * @param writer       log writer which can have sink swapped out
     * @param directLogger logger which writes to original sink
     */
    public LoggingManagerImpl(
            final OverridableStreamingLogWriter writer,
            final ExecutionLogger directLogger,
            final SimplePluginProviderLoader<LogFilterPlugin> pluginLoader,
            final List<PluginConfiguration> globalPluginConfigs
    )
    {
        this.writer = writer;
        this.directLogger = directLogger;
        this.pluginLoader = pluginLoader;
        this.globalPluginConfigs = globalPluginConfigs;
    }

    @Override
    public LoggingManager createManager(
            final List<PluginConfiguration> globalPluginConfigs
    )
    {
        return new LoggingManagerImpl(writer, directLogger, pluginLoader, globalPluginConfigs);
    }

    @Override
    public PluginLoggingManager createPluginLogging(ExecutionContext context, StepExecutionItem step) {
        MyPluginLoggingManager myPluginLoggingManager = new MyPluginLoggingManager(
                new PluginFilteredStreamingLogWriter(
                        writer.getWriter(),
                        context,
                        directLogger
                )
        );

        installPlugins(myPluginLoggingManager, globalPluginConfigs, context);

        if(step!=null) {
            HasLoggingFilterConfiguration.of(step).ifPresent(filtered -> {
                if (null != filtered.getFilterConfigurations()) {
                    installPlugins(myPluginLoggingManager, filtered.getFilterConfigurations(), context);
                }
            });
        }

        return myPluginLoggingManager;
    }

    private void installPlugins(
            final MyPluginLoggingManager myPluginLoggingManager,
            final List<PluginConfiguration> filterConfigurations,
            ExecutionContext context
    )
    {
        for (PluginConfiguration pluginConfiguration : filterConfigurations) {

            boolean disabled = context
                .getPluginControlService()
                .isDisabledPlugin(
                    pluginConfiguration.getProvider(),
                    pluginConfiguration.getService()
                );
            if (disabled) {
                throw new RuntimeException(String.format(
                    "Could not configure log filter plugin, it is disabled via project configuration: %s",
                    pluginConfiguration.getProvider()
                ));
            }
            LogFilterPlugin load = pluginLoader.load(
                    pluginConfiguration.getProvider(),
                    pluginConfiguration.getConfiguration()
            );
            if (load != null) {
                myPluginLoggingManager.installPlugin(load);
            }else{
                throw new RuntimeException("Could not configure log filter plugin: " +
                                           pluginConfiguration +
                                           ": not found");
            }
        }
    }


    private class MyPluginLoggingManager implements PluginLoggingManager {
        private final PluginFilteredStreamingLogWriter pluginFilteredStreamingLogWriter;
        boolean pluginsAdded = false;
        int pluginCount=0;

        MyPluginLoggingManager(final PluginFilteredStreamingLogWriter pluginFilteredStreamingLogWriter) {
            this.pluginFilteredStreamingLogWriter = pluginFilteredStreamingLogWriter;
        }

        private void installPlugin(final LogFilterPlugin plugin) {
            pluginFilteredStreamingLogWriter.addPlugin(plugin);
            pluginCount++;
            pluginsAdded = true;
        }

        @Override
        public <T> T runWith(final Supplier<T> supplier) {
            begin();
            try {
                return supplier.get();
            } finally {
                end();
            }
        }

        @Override
        public void begin() {
            if (pluginsAdded) {
                writer.setOverride(pluginFilteredStreamingLogWriter);
            }else{
                writer.pushEmpty();
            }
        }


        @Override
        public void end() {
            if (pluginsAdded) {
                writer.removeOverride();
                pluginFilteredStreamingLogWriter.close();
            }else{
                writer.removeOverride();
            }
        }
    }
}
