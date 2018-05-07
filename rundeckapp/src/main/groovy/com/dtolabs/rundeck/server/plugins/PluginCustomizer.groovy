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

package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.server.plugins.builder.ScriptPluginBuilder
import org.springframework.scripting.groovy.GroovyObjectCustomizer

/**
 * PluginCustomizer provides the Rundeck groovy script plugin DSL to define a plugin builder by use of a
 * `rundeckPlugin` method.
 * Created by greg
 * Date: 4/16/13
 * Time: 3:19 PM
 */
class PluginCustomizer implements GroovyObjectCustomizer {

    public void customize(GroovyObject goo) {
        if (goo instanceof Script) {
            goo.metaClass.rundeckPlugin = { Class clazz, Closure clos ->
                def builder = ScriptPluginBuilder.forPluginClass(clazz, goo.class.name)
                if(builder){
                    clos.delegate = builder
                    clos.resolveStrategy = Closure.DELEGATE_FIRST
                    clos.call()
                    return builder
                }
                return goo;
            }
        }
    }
}
