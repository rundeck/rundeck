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
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import groovy.transform.ToString

import java.util.regex.Pattern

/**
 * @author greg
 * @since 5/16/17
 */

@Plugin(name = MaskPasswordsFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Mask Passwords',
        description = 'Masks secure input option values from being emitted in the logs.')
@ToString(includeNames = true)
class MaskPasswordsFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'mask-passwords'
    Pattern regex;
    private boolean enabled;

    @Override
    void init(final PluginLoggingContext context) {
        def privdata = context.privateDataContext?.values()?.collect {
            it.values()
        }?.flatten()?.collect {
            Pattern.quote(it)
        }

        def strings = context.dataContext['secureOption']?.values()?.collect {
            Pattern.quote(it)
        }
        if (!privdata) {
            privdata = []
        }
        if (strings) {
            privdata.addAll(strings)
        }
        if (privdata.findAll { it }) {
            def mask = privdata.findAll { it }.join('|')
            regex = Pattern.compile('(' + mask + ')')
            enabled = true;
        } else {
            enabled = false
        }
    }

    @Override
    void handleEvent(final LogFilterPlugin.Control control, final LogEventControl event) {
        if (enabled) {
            def message = event.message
            if (event.eventType == 'log' && message) {
                String remessage = message.replaceAll(regex, '*****')
                event.setMessage(remessage)
            }
        }
    }

    @Override
    void complete(final LogFilterPlugin.Control control) {

    }
}
