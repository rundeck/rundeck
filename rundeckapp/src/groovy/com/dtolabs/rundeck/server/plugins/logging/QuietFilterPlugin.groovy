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
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author greg
 * @since 6/5/17
 */

@Plugin(name = QuietFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Quiet Output',
        description = '''Quiets all output which does or does not match a certain pattern.

Any log output that is chosen will be changed to VERBOSE level logging, which 
will only show up when running in DEBUG mode.''')
class QuietFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'quiet-output'

    @PluginProperty(
            title = "Pattern",
            description = '''Regular Expression to test.

See the [Java Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) documentation.''',
            required = false,
            validatorClass = SimpleDataFilterPlugin.RegexValidator
    )
    String regex


    @PluginProperty(
            title = 'Quiet Matched Output',
            description = '''If true, quiet matching lines. Otherwise quiet non-matching lines''',
            defaultValue = 'false'
    )
    Boolean quietMatch


    private Pattern dataPattern

    @Override
    void init(final PluginLoggingContext context) {
        dataPattern = Pattern.compile(regex)
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.eventType == 'log' && event.loglevel == LogLevel.NORMAL) {
            Matcher match = dataPattern.matcher(event.message)
            def matches = match.find()
            if (matches && quietMatch || !matches && !quietMatch) {
                event.quiet();
            }
        }
    }
}
