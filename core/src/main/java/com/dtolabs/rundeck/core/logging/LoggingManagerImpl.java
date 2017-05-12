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

package com.dtolabs.rundeck.core.logging;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext;
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin;

import java.util.Map;

/**
 * Creates plugin logging managers that can override the thread's log sink
 *
 * @author greg
 * @since 5/11/17
 */
public class LoggingManagerImpl implements LoggingManager {
    final OverridableStreamingLogWriter writer;
    final ExecutionLogger directLogger;

    /**
     * @param writer       log writer which can have sink swapped out
     * @param directLogger logger which writes to original sink
     */
    public LoggingManagerImpl(
            final OverridableStreamingLogWriter writer,
            final ExecutionLogger directLogger
    )
    {
        this.writer = writer;
        this.directLogger = directLogger;
    }

    @Override
    public PluginLoggingManager createPluginLogging(ExecutionContext context) {
        return new MyPluginLoggingManager(new PluginFilteredStreamingLogWriter(
                writer.getWriter(),
                context,
                directLogger
        ));
    }


    private class MyPluginLoggingManager implements PluginLoggingManager {
        private final PluginFilteredStreamingLogWriter pluginFilteredStreamingLogWriter;

        public MyPluginLoggingManager(final PluginFilteredStreamingLogWriter pluginFilteredStreamingLogWriter) {
            this.pluginFilteredStreamingLogWriter = pluginFilteredStreamingLogWriter;
        }

        @Override
        public void installPlugin(final LogFilterPlugin plugin) {
            pluginFilteredStreamingLogWriter.addPlugin(plugin);
        }

        @Override
        public void begin() {
            writer.setOverride(pluginFilteredStreamingLogWriter);
        }

        @Override
        public void end() {
            writer.removeOverride();
        }
    }
}
