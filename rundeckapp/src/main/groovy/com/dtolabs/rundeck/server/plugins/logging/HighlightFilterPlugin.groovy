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

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author greg
 * @since 6/5/17
 */

@Plugin(name = HighlightFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Highlight Output',
        description = '''Highlights all output which matches the given reqular expression.''')
class HighlightFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'highlight-output'
    @PluginProperty(
            title = "Pattern",
            description = '''Regular Expression to test. Use groups to selectively highlight.

Use a non-grouped pattern to highlight entire match:

* regex: `test`
* message: `this is a test`
* result: this is a *test*

Use regex groups to only highlight grouped sections:

* regex: `this (is) a (test)`
* result: this *is* a *test*

See the [Java Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) documentation.''',
            required = false,
            validatorClass = SimpleDataFilterPlugin.RegexValidator
    )
    String regex


    @PluginProperty(
            title = "Foreground Color",
            description = "ANSI color applied to replacement text.",
            required = false
    )
    @SelectValues(values = [
            'green',
            'red',
            'yellow',
            'blue',
            'magenta',
            'cyan',
            'white',
    ])
    String fgcolor
    @PluginProperty(
            title = "Background Color",
            description = "ANSI color applied to replacement text background.",
            required = false
    )
    @SelectValues(values = [
            'black',
            'red',
            'green',
            'yellow',
            'blue',
            'magenta',
            'cyan',
    ])
    String bgcolor
    @PluginProperty(
            title = "Mode",
            description = "ANSI color mode applied to replacement text.",
            required = false
    )
    @SelectValues(values = [
            'bold',
            'underline',
            'blink',
            'reverse'
            /**
             *    0: 'mode-normal',
             1: 'mode-bold',
             4: 'mode-underline',
             5: 'mode-blink',
             7: 'mode-reverse',
             */
    ])
    String mode

    private Pattern dataPattern
    private String mark = ''
    private String endMark = MaskPasswordsFilterPlugin.RESET
    String testMark

    static def BGCOLORS = [
            black  : '40',
            red    : '41',
            green  : '42',
            yellow : '43',
            blue   : '44',
            magenta: '45',
            cyan   : '46',
            white  : '47',
//            default  : MaskPasswordsFilterPlugin.ESC + '[49',
    ]
    static def MODES = [
            bold     : '1',
            underline: '4',
            blink    : '5',
            reverse  : '7',
    ]

    @Override
    void init(final PluginLoggingContext context) {

        dataPattern = Pattern.compile(regex)
        mark = ''
        if (fgcolor) {
            mark += MaskPasswordsFilterPlugin.ESC + MaskPasswordsFilterPlugin.COLORS[fgcolor]
        }
        if (bgcolor) {
            mark += (mark.length() ? ';' : MaskPasswordsFilterPlugin.ESC + '[') + BGCOLORS[bgcolor]
        }
        if (mode) {
            mark += (mark.length() ? ';' : MaskPasswordsFilterPlugin.ESC + '[') + MODES[mode]
        }
        if (mark.length()) {
            mark += 'm'
        }
        if (testMark) {
            mark += testMark
            endMark = testMark
        }
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.eventType == 'log' && event.loglevel == LogLevel.NORMAL && event.message) {
            Matcher match = dataPattern.matcher(event.message)
            int start = 0
            def sb = new StringBuilder()
            boolean found=false
            while (match.find()) {
                found=true
                if (match.groupCount()) {
                    match.group(1)

                    for (int i = 1; i <= match.groupCount(); i++) {
                        sb.append(event.message.substring(start, match.start(i)))
                        sb.append(mark)
                        sb.append(event.message.substring(match.start(i), match.end(i)))
                        sb.append(endMark)
                        start = match.end(i)
                    }
                } else {
                    sb.append(event.message.substring(0, match.start()))
                    sb.append(mark)
                    sb.append(event.message.substring(match.start(), match.end()))
                    sb.append(endMark)
                    start = match.end()
                }
            }
            sb.append(event.message.substring(start, event.message.length()))
            if(found){
                event.message = sb.toString()
            }
        }
    }
}
