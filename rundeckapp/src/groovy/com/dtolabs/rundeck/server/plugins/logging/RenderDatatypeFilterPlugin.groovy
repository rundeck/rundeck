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

package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin

/**
 * @author greg
 * @since 5/26/17
 */
@Plugin(name = RenderDatatypeFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Render Formatted Data',
        description = '''Allows marking formatted data as a certain data type, for rendering in the Log Output.

Some supported datatypes:

* `application/json` [JSON][] (synonyms: `json`)
* `application/x-java-properties` [Java Properties][] (synonyms: `properties`)
* `text/csv` CSV (synonyms: `csv`)
* `text/html` HTML (synonyms: `html`)
* `text/x-markdown` [Markdown][] (synonyms: `markdown`,`md`)

[JSON]: http://json.org
[Markdown]: https://en.wikipedia.org/wiki/Markdown
[Java Properties]: https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html#load(java.io.Reader)

To mark a section of output with a datatype, echo this marker defining it:

    #BEGIN:RUNDECK:DATATYPE:<datatype>

Replacing `<datatype>` with one of the supported data types.

You can mark the section as ending by echoing:

    #END:RUNDECK:DATATYPE

Otherwise, when the step ends the plugin will treat it as ended.

You can also choose a value for the `Data Type` property, to preset
a datatype to use for the entire output log data.  If this is set, then
no "BEGIN" marker is looked for.

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

    static final def KNOWN_TYPES = [
            JSON_TYPE,
            PROPERTIES_TYPE,
            CSV_TYPE,
            HTML_TYPE,
            MARKDOWN_TYPE
    ]
    static final def SYNONYMS = [
            json      : JSON_TYPE,
            properties: PROPERTIES_TYPE,
            csv       : CSV_TYPE,
            html      : HTML_TYPE,
            markdown  : MARKDOWN_TYPE,
            md        : MARKDOWN_TYPE,
    ]
    public static final String JSON_TYPE = 'application/json'
    public static final String PROPERTIES_TYPE = 'application/x-java-properties'
    public static final String CSV_TYPE = 'text/csv'
    public static final String HTML_TYPE = 'text/html'
    public static final String MARKDOWN_TYPE = 'text/x-markdown'

    private boolean started = false;

    @PluginProperty(
            title = "Data type",
            description = '''Enter a data type to use by default for all output from the
 step.  If not set, the BEGIN and END markers will be looked for.''',
            required = false
    )
    @SelectValues(
            values = ['application/json', 'application/x-java-properties', 'text/csv', 'text/html', 'text/x-markdown'],
            freeSelect = true
    )
    @SelectLabels(values = ['JSON', 'Java Properties', 'CSV', 'HTML', 'Markdown'])
    String datatype = null
    private StringBuilder buffer;

    @Override
    void init(final PluginLoggingContext context) {
        started = false
        buffer = new StringBuilder()
        if (datatype) {
            started = true
        }
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.getEventType() == 'log' && event.loglevel == LogLevel.NORMAL) {
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
        if (started && datatype && buffer.length() > 0) {
            if (buffer.length() > 0) {
                buffer.append("\n")
            }
            context.log(
                    2,
                    buffer.toString(),
                    [
                            'content-data-type': SYNONYMS[datatype.toLowerCase()] ?: datatype
                    ]
            )
        }
        buffer = new StringBuilder()
        started = false
        datatype = null
    }

}
