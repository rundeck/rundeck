/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package webhooks.plugins

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

/**
 * Creates rundeck plugins as bean instances and registers them to the registry
 * @param <T>
 */
class PluginFactoryBean<T> implements FactoryBean<T>, InitializingBean, BeanNameAware {

    Closure maker
    Class<?> objectType
    boolean singleton
    String beanName
    String provider

    @Autowired
    PluginRegistry rundeckPluginRegistry

    PluginFactoryBean(final Class<?> objectType) {
        this.objectType = objectType
    }

    @Override
    void afterPropertiesSet() throws Exception {
        def annotation = objectType.getAnnotation(Plugin)
        if (annotation) {
            rundeckPluginRegistry.pluginRegistryMap.putIfAbsent(annotation.name(), beanName)
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

