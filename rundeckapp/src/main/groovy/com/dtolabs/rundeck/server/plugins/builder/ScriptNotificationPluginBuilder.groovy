/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Builds a {@link NotificationPlugin} from the groovy DSL for rundeck plugins.  Allows
 * Notification trigger X to be defined by calling a method "onX" and passing a closure.  The closure
 * can have 0-3 arguments.
 * User: greg
 * Date: 4/16/13
 * Time: 4:32 PM
 */
class ScriptNotificationPluginBuilder extends ScriptPluginBuilder implements PluginBuilder<NotificationPlugin>{
    static Logger logger = Logger.getLogger(ScriptNotificationPluginBuilder)
    Map<String, Closure> triggers=[:]
    ScriptNotificationPluginBuilder(Class clazz,String name) {
        super(clazz,name)
    }

    @Override
    public NotificationPlugin buildPlugin() {
        //return a new NotificationPlugin
        return new ScriptNotificationPlugin(triggers, descriptionBuilder.build())
    }

    @Override
    Object invokeMethod(String name, Object args) {
        List list = InvokerHelper.asList(args);
        if(name.startsWith('on') && list.size()==1 && list[0] instanceof Closure){
            if(!ScriptNotificationPlugin.validNotificationClosure(list[0])){
                logger.error("Invalid trigger closure: ${name}, unexpected parameter set")
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }
            triggers[name.substring(2)]=list[0]
            return true
        }
        throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
    }
}
