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
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin

import java.util.regex.Pattern

/**
 * @author greg
 * @since 6/5/17
 */

@Plugin(name = QuietFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Quiet Output',
        description = '''Quiets all output which does or does not match a certain pattern by changing its log level.''')
class QuietFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'quiet-output'

    @PluginProperty(
            title = "Pattern",
            description = '''Regular Expression to test. If blank, all lines will match.

See the [Java Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) documentation.''',
            required = false,
            validatorClass = SimpleDataFilterPlugin.RegexValidator
    )
    String regex

    @PluginProperty(
            title = "Test Log Level",
            description = '''Test the pattern against only log lines of the given log level. (Default: normal).

If `any` is specified, all log levels will be tested.''',
            defaultValue = 'normal',
            required = false
    )
    @SelectValues(values = ['error', 'warn', 'normal', 'verbose', 'debug', 'all'])
    String matchLoglevel

    @PluginProperty(
            title = 'Quiet Matched Output',
            description = '''If true, quiet matching lines. Otherwise quiet non-matching lines''',
            defaultValue = 'false'
    )
    Boolean quietMatch

    @PluginProperty(
            title = "Result Log Level",
            description = "Quieted lines will be changed to this log level. (Default: verbose)",
            defaultValue = 'verbose',
            required = false
    )
    @SelectValues(values = ['error', 'warn', 'normal', 'verbose', 'debug'])
    String loglevel


    private Pattern dataPattern
    private LogLevel matchLevel
    private LogLevel resultLevel

    @Override
    void init(final PluginLoggingContext context) {
        dataPattern = regex ? Pattern.compile(regex) : null
        matchLevel = (matchLoglevel == 'all' || !matchLoglevel) ? null : LogLevel.valueOf(matchLoglevel.toUpperCase())
        resultLevel = (loglevel) ? LogLevel.valueOf(loglevel.toUpperCase()) : null
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.eventType == 'log' && matchLevel(event)) {
            def matches = dataPattern ? dataPattern.matcher(event.message).find() : true
            if (matches && quietMatch || !matches && !quietMatch) {
                if (resultLevel) {
                    event.loglevel = resultLevel
                } else {
                    event.quiet();
                }
            }
        }
    }

    private boolean matchLevel(LogEventControl event) {
        !matchLevel || event.loglevel == matchLevel
    }
}
