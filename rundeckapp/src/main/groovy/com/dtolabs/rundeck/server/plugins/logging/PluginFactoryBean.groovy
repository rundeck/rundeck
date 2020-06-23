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

package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

/**
 * Defines a Rundeck Plugin factory, which automatically sets the
 * pluginRegistryMap to map the provider name to the bean name
 * @author greg
 * @since 5/25/17
 */
class PluginFactoryBean<T> implements FactoryBean<T>, InitializingBean, BeanNameAware {

    Closure maker
    Class<?> objectType
    boolean singleton
    String beanName
    String provider

    @Autowired
    RundeckPluginRegistry rundeckPluginRegistry

    PluginFactoryBean(final Class<?> objectType) {
        this.objectType = objectType
    }

    @Override
    void afterPropertiesSet() throws Exception {
        def annotation = objectType.getAnnotation(Plugin)
        if (annotation) {
            rundeckPluginRegistry.registerPlugin(annotation.service(), annotation.name(), beanName)
        }
    }

    @Override
    T getObject() throws Exception {
        if (maker) {
            return (T) maker.call()
        } else {
            //no-arg constructor
            return objectType.getConstructor().newInstance()
        }
    }
}
