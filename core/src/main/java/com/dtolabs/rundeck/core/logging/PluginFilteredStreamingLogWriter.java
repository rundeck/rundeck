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

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContext;
import com.dtolabs.rundeck.core.dispatcher.MultiDataContext;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext;
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin;

import java.io.IOException;
import java.util.*;

/**
 * @author greg
 * @since 5/11/17
 */
public class PluginFilteredStreamingLogWriter extends FilterStreamingLogWriter {
    final List<LogFilterPlugin> plugins;
    final ExecutionContext context;
    final ExecutionLogger directLogger;

    public PluginFilteredStreamingLogWriter(
            final StreamingLogWriter writer,
            ExecutionContext context,
            ExecutionLogger directLogger
    )
    {
        super(writer);
        this.context = context;
        this.directLogger = directLogger;
        plugins = new ArrayList<>();
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

        public EventControl(
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
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public LogEventControl setEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        @Override
        public Date getDatetime() {
            return datetime;
        }

        public void setDatetime(Date datetime) {
            this.datetime = datetime;
        }

        @Override
        public LogLevel getLoglevel() {
            return loglevel;
        }

        @Override
        public LogEventControl setLoglevel(LogLevel loglevel) {
            this.loglevel = loglevel;
            return this;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public LogEventControl setMessage(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }

        @Override
        public LogEventControl addMetadata(final Map<String, String> data) {
            this.metadata.putAll(data);
            return this;
        }

        @Override
        public LogEventControl addMetadata(final String key, final String value) {
            this.metadata.put(key, value);
            return this;
        }

        @Override
        public void emit() {
            state = ControlState.EMIT;
        }

        @Override
        public void quell() {
            state = ControlState.QUELL;
        }

        @Override
        public void remove() {
            state = ControlState.REMOVE;
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
    }

    static enum ControlState {
        EMIT,
        QUELL,
        REMOVE
    }

    static class FilterControl implements LogFilterPlugin.Control {
        List<LogEvent> events = new ArrayList<>();


        @Override
        public void addEvent(final LogEvent event) {
            events.add(event);
        }


    }

    @Override
    public void addEvent(final LogEvent orig) {
        ControlState state = ControlState.EMIT;
        EventControl eventControl = EventControl.with(orig);
//        Map<String, String> origmeta = new HashMap<>(orig.getMetadata());

        List<LogEvent> toAdd = new ArrayList<>();
        for (LogFilterPlugin plugin : plugins) {

            FilterControl control = new FilterControl();
            //reset state
            eventControl.emit();
            plugin.handleEvent(control, eventControl);
            toAdd.addAll(control.events);

            if (eventControl.state == ControlState.REMOVE) {
                state = eventControl.state;
                break;
            } else if (state == ControlState.EMIT && eventControl.state == ControlState.QUELL) {
                state = eventControl.state;
            }
        }
        if (state == ControlState.EMIT) {
            getWriter().addEvent(eventControl);
        }
        emitExtraEvents(toAdd);
    }

    public void emitExtraEvents(final List<LogEvent> toAdd) {
        //TODO: use logger?
        for (LogEvent logEvent : toAdd) {
            getWriter().addEvent(logEvent);
        }
    }

    @Override
    public void close() {
        List<LogEvent> toAdd = new ArrayList<>();
        for (LogFilterPlugin plugin : plugins) {
            FilterControl control = new FilterControl();
            plugin.complete(control);
            toAdd.addAll(control.events);
        }
        emitExtraEvents(toAdd);
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

        public MyLoggingContext(
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
        public void event(final String eventType, final String message, final Map eventMeta) {
            logger.event(eventType, message, eventMeta);
        }

        @Override
        public SharedOutputContext getOutputContext() {
            return outputContext;
        }
    }

    public void addPlugin(final LogFilterPlugin plugin) {
        plugin.init(new MyLoggingContext(
                context.getOutputContext(),
                directLogger,
                context.getDataContext(),
                context.getPrivateDataContext(),
                context.getSharedDataContext()
        ));
        plugins.add(plugin);
    }
}
