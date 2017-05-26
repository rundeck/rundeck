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

import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin

/**
 * @author greg
 * @since 5/26/17
 */
@Plugin(name = RenderDatatypeFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Render Formatted Data',
        description = '''Allows marking formatted data as a certain data type, for rendering in the Log Output.

Some supported datatypes:

* `application/json` [JSON][]
* `application/x-java-properties` [Java Properties][]
* `text/csv` CSV 
* `text/html` HTML
* `text/x-markdown` [Markdown][]

[JSON]: http://json.org
[Markdown]: https://en.wikipedia.org/wiki/Markdown
[Java Properties]: https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html#load(java.io.Reader)

The data can then be rendered in the Rundeck Log output GUI.
The specific renderer for the data type is determined by available
ViewConverter plugins.

For example, you can emit JSON data, and prefix it with:

    echo #BEGIN:RUNDECK:DATATYPE:application/json

Then emit json data (only)

    cat file.json

Then END the datatype:

    echo #END:RUNDECK:DATATYPE

The log output will then capture all of the JSON data in a single
log event, and mark it as `application/json` data type.

''')
class RenderDatatypeFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'render-datatype'

    public static final String START_PREFIX = '#BEGIN:RUNDECK:DATATYPE:'
    public static final String END_PREFIX = '#END:RUNDECK:DATATYPE'

    private boolean started = false;
    private String datatype = null
    private StringBuilder buffer;

    @Override
    void init(final PluginLoggingContext context) {
        started = false
        datatype = null
        buffer = new StringBuilder()
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.getEventType() == 'log' && event.message) {
            if (!started) {
                if (event.message.startsWith(START_PREFIX)) {
                    started = true;
                    datatype = event.message.substring(START_PREFIX.length()).trim()
                    //reduce event level for the log output
                    event.loglevel = LogLevel.DEBUG
                }
            } else if (event.message.startsWith(END_PREFIX)) {
                event.loglevel = LogLevel.DEBUG
                emitData(context)
            } else {
                //add to buffer
                if (buffer.length() > 0) {
                    buffer.append("\n")
                }
                buffer.append(event.message)
                event.loglevel = LogLevel.DEBUG
            }
        }
    }

    @Override
    void complete(final PluginLoggingContext context) {

        //finish the log as the given data
        emitData(context)
    }

    private emitData(PluginLoggingContext context) {
        if (started && datatype) {
            if (buffer.length() > 0) {
                buffer.append("\n")
            }
            context.log(
                    2,
                    buffer.toString(),
                    [
                            'content-data-type': datatype
                    ]
            )
        }
        started = false
        datatype = null
    }
}
