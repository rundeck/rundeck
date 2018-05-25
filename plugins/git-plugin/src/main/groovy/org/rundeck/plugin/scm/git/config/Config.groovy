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

package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import groovy.transform.CompileStatic
import org.rundeck.plugin.scm.git.BuilderUtil

/**
 * Base configuration class, creates plugin properties from annotations and creates instances
 */
class Config {
    Map<String, Object> otherInput
    Map<String, String> rawInput

    /**
     * @param classes
     * @return create a property list by introspection of the classes
     */
    static List<Property> listProperties(Class<?>... classes) {
        classes.collect {
            PluginAdapterUtility.buildFieldProperties(it)
        }.flatten() as List
    }

    /**
     * Configure a config object given the input
     * @param config config option
     * @param input input values
     * @throws ScmPluginInvalidInput
     */
    static void configure(Config config, final Map<String, String> input) throws ScmPluginInvalidInput {
        Map<String,Object> unused = PluginAdapterUtility.configureObjectFieldsWithProperties(config, input as Map<String,Object>)
        listProperties(config.class).findAll { it.required }.each { prop ->
            //verify required input
            if (!input[prop.name]) {
                throw new ScmPluginInvalidInput("${prop.name} cannot be null", Validator.errorReport(prop.name,"cannot be null"))
            }
        }
        config.otherInput = unused
        config.rawInput = input
    }

    /**
     * Create a new config object
     * @param clazz config object class
     * @param input input values
     * @return new instance configured with the input
     * @throws ScmPluginInvalidInput
     */
    static <T extends Config> T create(Class<T> clazz, final Map<String, String> input) throws ScmPluginInvalidInput {
        T object = clazz.getDeclaredConstructor().newInstance()
        configure(object, input)
        object
    }

    /**
     * Replace an entry in the list with the given property by matching the property name
     * @param list
     * @param newProperty
     * @return
     */
    static List<Property> substituteDefaultValue(List<Property> list, String name, String newDefaultValue) {
        list.collect {
            if (it.name == name) {
                BuilderUtil.property(it) {
                    defaultValue newDefaultValue
                }
            } else {
                it
            }
        }
    }


    @Override
    public String toString() {
        return "${this.class.name}{" +
                "otherInput=" + otherInput +
                ", rawInput=" + rawInput +
                '}';
    }
}
