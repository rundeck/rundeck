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

package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.dispatcher.DataContext
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.dispatcher.MutableDataContext
import com.dtolabs.rundeck.core.execution.workflow.OutputContext
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.databind.ObjectMapper

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author greg
 * @since 5/17/17
 */
@Plugin(name = SimpleDataFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Key Value Data',
        description = '''Captures simple Key/Value data using a simple text format.\n\n
To produce a key/value entry, echo a line similar to this:

    RUNDECK:DATA:(key) = (value)

Where `(key)` is the key name, and `(value)` is the value.
''')

class SimpleDataFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'key-value-data'
    public static final String PATTERN = '^RUNDECK:DATA:(.+)\\s*=\\s*(.+)$'


    Pattern dataPattern;
    OutputContext outputContext
    private ObjectMapper mapper

    @Override
    void init(final PluginLoggingContext context) {
        dataPattern = Pattern.compile(PATTERN)
        outputContext = context.getOutputContext()
        mapper = new ObjectMapper()
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.eventType == 'log' && event.message?.length() > 15) {
            Matcher match = dataPattern.matcher(event.message)
            if (match.matches()) {
                def key = match.group(1)
                def value = match.group(2)
                if (key && value) {
                    outputContext.addOutput("data", key, value)
                }

                context.event(
                        'log',
                        mapper.writeValueAsString([data: [(key): value]]),
                        ['content-data-type': 'application/json']
                )
            }
        }
    }

}
