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

import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.InvokerHelper

class LogFilterPluginBuilder extends ScriptPluginBuilder implements PluginBuilder<LogFilterPlugin> {
    static Logger logger = Logger.getLogger(StreamingLogWriterPluginBuilder)
    Map<String, Closure> handlers = [:]

    LogFilterPluginBuilder(Class clazz, String name) {
        super(clazz, name)
    }

    @Override
    LogFilterPlugin buildPlugin() {
        return new ScriptLogFilterPlugin(handlers, descriptionBuilder.build())
    }

    @Override
    Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if (name == 'init' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptLogFilterPlugin.validInitCompleteClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'complete' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptLogFilterPlugin.validInitCompleteClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'handleEvent' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptLogFilterPlugin.validEventClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
    }
}
