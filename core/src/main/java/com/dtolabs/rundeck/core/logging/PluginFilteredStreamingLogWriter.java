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

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext;
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin;

import java.io.IOException;
import java.util.*;

/**
 * Filters log output through log filter plugins
 * @author greg
 * @since 5/11/17
 */
public class PluginFilteredStreamingLogWriter extends FilterStreamingLogWriter {
    private final List<LogFilterPlugin> plugins;
    private final MyLoggingContext myLoggingContext;

    /**
     * Create
     * @param writer sink for filtered log events
     * @param context context
     * @param directLogger logger for plugin logging to bypass filtering
     */
    PluginFilteredStreamingLogWriter(
            final StreamingLogWriter writer,
            ExecutionContext context,
            ExecutionLogger directLogger
    )
    {
        super(writer);
        plugins = new ArrayList<>();
        myLoggingContext = new MyLoggingContext(
                context.getOutputContext(),
                directLogger,
                context.getDataContextObject(),
                context.getPrivateDataContextObject(),
                context.getSharedDataContext()
        );
    }


    @Override
    public void openStream() throws IOException {
        //getWriter().openStream();
    }

    static class EventControl implements LogEventControl {
        private String eventType;
        private Date datetime;
        private LogLevel loglevel;
        private String message;
        private Map<String, String> metadata;
        ControlState state = ControlState.EMIT;
        private boolean modified;

        EventControl(
                final String eventType,
                final Date datetime,
                final LogLevel loglevel,
                final String message,
                final Map<String, String> metadata
        )
        {
            this.eventType = eventType;
            this.datetime = datetime;
            this.loglevel = loglevel;
            this.message = message;
            this.metadata = metadata;
            if(this.metadata==null){
                this.metadata = new HashMap<>();
            }
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public LogEventControl setEventType(String eventType) {
            modified |= !this.eventType.equals(eventType);
            this.eventType = eventType;
            return this;
        }

        @Override
        public Date getDatetime() {
            return datetime;
        }

        @Override
        public LogLevel getLoglevel() {
            return loglevel;
        }

        @Override
        public LogEventControl setLoglevel(LogLevel loglevel) {
            modified |= !this.loglevel.equals(loglevel);
            this.loglevel = loglevel;
            return this;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public LogEventControl setMessage(String message) {
            modified |= !this.message.equals(message);
            this.message = message;
            return this;
        }

        @Override
        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            modified = true;
            this.metadata = metadata;
        }

        @Override
        public LogEventControl addMetadata(final Map<String, String> data) {
            this.metadata.putAll(data);
            modified = true;
            return this;
        }

        @Override
        public LogEventControl addMetadata(final String key, final String value) {
            this.metadata.put(key, value);
            modified = true;
            return this;
        }

        @Override
        public LogEventControl emit() {
            state = ControlState.EMIT;
            return this;
        }

        @Override
        public LogEventControl quell() {
            modified = true;
            state = ControlState.QUELL;
            return this;
        }

        @Override
        public LogEventControl remove() {
            modified = true;
            state = ControlState.REMOVE;
            return this;
        }

        @Override
        public LogEventControl quiet() {
            modified = true;
            state = ControlState.QUIET;
            return this;
        }

        static EventControl with(LogEvent event) {
            return new EventControl(
                    event.getEventType(),
                    event.getDatetime(),
                    event.getLoglevel(),
                    event.getMessage(),
                    event.getMetadata()
            );
        }

        public boolean isModified() {
            return modified;
        }
    }

    private enum ControlState {
        EMIT,
        QUELL,
        QUIET,
        REMOVE
    }

    @Override
    public void addEvent(final LogEvent orig) {
        if (!"log".equals(orig.getEventType())) {
            getWriter().addEvent(orig);
            return;
        }
        ControlState state = ControlState.EMIT;
        EventControl eventControl = EventControl.with(orig);

        for (LogFilterPlugin plugin : plugins) {
            //reset state
            eventControl.emit();
            plugin.handleEvent(myLoggingContext, eventControl);

            if (eventControl.state == ControlState.REMOVE) {
                state = eventControl.state;
                break;
            } else if (state == ControlState.EMIT && eventControl.state != ControlState.EMIT) {
                state = eventControl.state;
            }
        }
        if (state != ControlState.REMOVE && state != ControlState.QUELL) {
            if (state == ControlState.QUIET) {
                eventControl.setLoglevel(LogLevel.VERBOSE);
            }
            getWriter().addEvent(eventControl.modified ? eventControl : orig);
        }
    }

    @Override
    public void close() {
        for (LogFilterPlugin plugin : plugins) {
            plugin.complete(myLoggingContext);
        }
    }

    private static class MyLoggingContext implements PluginLoggingContext {
        SharedOutputContext outputContext;
        ExecutionLogger logger;
        DataContext dataContext;
        DataContext privateDataContext;
        MultiDataContext<ContextView, DataContext> sharedDataContext;

        /**
         * Return data context set
         *
         * @return map of data contexts keyed by name
         */
        public DataContext getDataContext() {
            return dataContext;
        }

        /**
         * @return the scoped context data keyed by scope
         */
        public MultiDataContext<ContextView, DataContext> getSharedDataContext() {
            return sharedDataContext;
        }

        /**
         * @return the data context in the private scope
         */
        public DataContext getPrivateDataContext() {
            return privateDataContext;
        }

        MyLoggingContext(
                final SharedOutputContext outputContext,
                final ExecutionLogger logger,
                final DataContext dataContext,
                final DataContext privateDataContext,
                final MultiDataContext<ContextView, DataContext> sharedDataContext
        )
        {
            this.outputContext = outputContext;
            this.logger = logger;
            this.dataContext = dataContext;
            this.privateDataContext = privateDataContext;
            this.sharedDataContext = sharedDataContext;
        }

        @Override
        public void log(final int level, final String message) {
            logger.log(level, message);
        }

        @Override
        public void log(final int level, final String message, final Map eventMeta) {
            logger.log(level, message, eventMeta);
        }

        @Override
        public void event(final String eventType, final String message, final Map eventMeta) {
            logger.event(eventType, message, eventMeta);
        }

        @Override
        public SharedOutputContext getOutputContext() {
            return outputContext;
        }
    }

    void addPlugin(final LogFilterPlugin plugin) {
        plugin.init(myLoggingContext);
        plugins.add(plugin);
    }
}
