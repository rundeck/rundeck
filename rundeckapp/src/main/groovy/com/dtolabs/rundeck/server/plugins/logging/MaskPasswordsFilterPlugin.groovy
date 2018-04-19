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

import com.dtolabs.rundeck.core.cli.CLIUtils
import com.dtolabs.rundeck.core.logging.LogEventControl
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
 * @since 5/16/17
 */

@Plugin(name = MaskPasswordsFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Mask Passwords',
        description = 'Masks secure input option values from being emitted in the logs.')
class MaskPasswordsFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'mask-passwords'

    @PluginProperty(
            title = "Replacement",
            description = "Text to replace secure values",
            defaultValue = "[SECURE]",
            required = true
    )
    String replacement

    @PluginProperty(
            title = "Color",
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
    String color

    Pattern regex;
    private boolean enabled;
    private String replacementQuoted
    static def ESC = '\u001B'
    static def RESET = ESC + '[0m'
//    def rgb={r,g,b-> 16 + b + 6*g + 36*r }
    static def COLORS = [
            red    :  '[31',
            green  :  '[32',
            yellow :  '[33',
            blue   :  '[34',
            magenta:  '[35',
            cyan   :  '[36',
            white  :  '[37',
    ]
    @Override
    void init(final PluginLoggingContext context) {
        def privdata = context.privateDataContext?.values()?.collect {
            it.values()
        }?.flatten()

        def strings = context.dataContext['secureOption']?.values()
        if (!privdata) {
            privdata = []
        }
        if (strings) {
            privdata.addAll(strings)
        }
        if (!privdata.findAll { it }) {
            enabled = false
            return
        }
        def mask = privdata.findAll { it }.collect {
            def vals = ["'" + it + "'", '"' + it + '"']
            def quoted = CLIUtils.quoteUnixShellArg(it)
            if (quoted != it && !vals.contains(quoted)) {
                vals << quoted
            }

            quoted = CLIUtils.escapeUnixShellChars(it, '"' + CLIUtils.UNIX_SHELL_CHARS_NO_QUOTES)
            if (quoted != it && !vals.contains(quoted)) {
                vals << ('"' + quoted + '"')
            }
            quoted = CLIUtils.escapeUnixShellChars(it, "'\\")
            if (quoted != it && !vals.contains(quoted)) {
                vals << "'" + quoted + "'"
            }
            vals << it
            vals
        }.flatten().collect { Pattern.quote(it) }.join('|')
        regex = Pattern.compile('(' + mask + ')')
        enabled = true;
        replacementQuoted = Matcher.quoteReplacement(replacement ?: '*****')
        if (color) {
            replacementQuoted = ESC+COLORS[color] + 'm' + replacementQuoted + RESET
        }
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (enabled) {
            def message = event.message
            if (event.eventType == 'log' && message) {
                String remessage = message.replaceAll(regex, replacementQuoted)
                event.setMessage(remessage)
            }
        }
    }

    @Override
    void complete(final PluginLoggingContext context) {

    }
}
