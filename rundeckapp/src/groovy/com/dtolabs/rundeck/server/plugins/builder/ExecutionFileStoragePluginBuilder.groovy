/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 12/6/13
 * Time: 2:57 PM
 */
class ExecutionFileStoragePluginBuilder extends ScriptPluginBuilder implements PluginBuilder<ExecutionFileStoragePlugin> {
    static Logger logger = Logger.getLogger(StreamingLogWriterPluginBuilder)
    Map<String, Closure> handlers = [:]

    ExecutionFileStoragePluginBuilder(String name) {
        super(name)
    }

    @Override
    ExecutionFileStoragePlugin buildPlugin() {
        if(handlers.storeMultiple){
            return new ScriptExecutionMultiFileStoragePlugin(handlers, descriptionBuilder.build())
        }else{
            return new ScriptExecutionFileStoragePlugin(handlers, descriptionBuilder.build())
        }
    }

    @Override
    Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if (name == 'available' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptExecutionFileStoragePlugin.validAvailableClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'store' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptExecutionFileStoragePlugin.validStoreClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'retrieve' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptExecutionFileStoragePlugin.validRetrieveClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }
        if (name == 'storeMultiple' && list.size() == 1 && list[0] instanceof Closure) {
            if (!ScriptExecutionMultiFileStoragePlugin.validStoreMultipleClosure(list[0])) {
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set: ${list[0].parameterTypes}")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            handlers[name] = list[0]
            return true
        }

        throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
    }
}
