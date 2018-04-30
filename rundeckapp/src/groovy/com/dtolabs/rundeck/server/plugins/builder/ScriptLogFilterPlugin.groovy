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

package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin

import org.apache.log4j.Logger

class ScriptLogFilterPlugin implements LogFilterPlugin, Describable, Configurable {
    static Logger logger = Logger.getLogger(ScriptLogFilterPlugin)
    Description description
    private Map<String, Closure> handlers
    Map configuration
    PluginLoggingContext context

    ScriptLogFilterPlugin(Map<String, Closure> handlers, Description description) {
        this.description = description
        this.handlers = handlers
    }

    @Override
    void configure(final Properties configuration) throws ConfigurationException {
        this.configuration = new HashMap(configuration)
    }

    @Override
    void init(final PluginLoggingContext context) {
        this.context = context
        if (!handlers.handleEvent) {
            throw new RuntimeException(
                "LogFilterPlugin: 'handleEvent' closure not defined for plugin ${description.name}"
            )
        }
        def closure = handlers.init
        if (!closure) {
            return
        }
        if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(context, configuration)
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
            def Closure newclos = closure.clone()
            newclos.delegate = [context: context, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(context)
        } else {
            logger.error(
                "LogFilterPlugin: 'init' closure signature invalid for plugin ${description.name}, cannot init"
            )
        }
    }

    @Override
    void complete(final PluginLoggingContext context) {
        def closure = handlers.complete
        if (!closure) {
            return
        }
        if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(context, configuration)
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
            def Closure newclos = closure.clone()
            newclos.delegate = [context: context, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(context)
        } else {
            logger.error(
                "LogFilterPlugin: 'complete' closure signature invalid for plugin ${description.name}, cannot complete"
            )
        }
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        def closure = handlers.handleEvent
        if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(context, event, configuration)
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.delegate = [context: context, event: event, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(context, event)
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = [context: context, event: event, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(event)
        } else {
            logger.error(
                "LogFilterPlugin: 'handleEvent' closure signature invalid for plugin ${description.name}, cannot handleEvent"
            )
        }
    }

    public static boolean validInitCompleteClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == PluginLoggingContext && closure.parameterTypes[1] == Map
        }else if (closure.getMaximumNumberOfParameters() == 1) {
            return closure.parameterTypes[0] == PluginLoggingContext || closure.parameterTypes[0] == Object
        }
        return false
    }

    public static boolean validEventClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[1] == LogEventControl
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[1] == LogEventControl
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return closure.parameterTypes[0] == LogEventControl || closure.parameterTypes[0] == Object
        }
        return false
    }
}
