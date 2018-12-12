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

import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin


class ScriptOptionValuesPlugin implements OptionValuesPlugin, Describable, Configurable {

    private HashMap config
    private Description description
    Map<String, Closure> handlers

    ScriptOptionValuesPlugin(Map<String, Closure> handlers, Description description) {
        this.handlers = handlers
        this.description = description
    }

    @Override
    void configure(final Properties configuration) throws ConfigurationException {
        this.config = new HashMap(configuration)
    }

    @Override
    Description getDescription() {
        return description
    }

    @Override
    List<OptionValue> getOptionValues(Map scriptConfig) {
        Closure cls = handlers["getOptionValues"]
        cls.delegate = [configuration: config]
        cls.resolveStrategy = Closure.DELEGATE_ONLY
        return cls.call(config)
    }
}
