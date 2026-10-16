/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package rundeckapp.init

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertySource

/**
 * Applies {@code quartz.*} settings from rundeck-config.properties to the Quartz scheduler.
 *
 * The quartz plugin builds its {@code quartzProperties} in doWithSpring, reading the {@code quartz}
 * config subtree and re-prefixing it as {@code org.quartz.*}. At that point rundeck-config.properties
 * has not been merged into the config the plugin reads, so the subtree comes back empty. An empty
 * subtree is not an error -- it just means no overrides -- so the scheduler silently came up on
 * Quartz's own defaults. {@code quartz.threadPool.threadCount=60} produced a pool of 10, capping
 * concurrent jobs at 10 with nothing logged.
 *
 * This runs as a BeanFactoryPostProcessor instead, after every bean definition is registered and
 * before the scheduler is instantiated, and reads the Environment rather than the application
 * config: the property source carrying rundeck-config.properties is added in
 * {@code Application.setEnvironment}, well before the context is refreshed, so it is always visible
 * here. It only adds what is configured, leaving the plugin's own values in place otherwise.
 *
 * @see rundeckapp.Application#setEnvironment
 */
@CompileStatic
@Slf4j
class QuartzPropertiesBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware {

    static final String SCHEDULER_BEAN = 'quartzScheduler'
    static final String QUARTZ_PROPERTIES = 'quartzProperties'
    static final String CONFIG_PREFIX = 'quartz.'
    static final String QUARTZ_PREFIX = 'org.quartz.'

    /**
     * Switches the plugin reads for itself, which are not Quartz properties. Forwarding them would
     * put keys Quartz does not know into its configuration without changing what the plugin does.
     */
    static final Set<String> PLUGIN_SWITCHES = [
            'autoStartup', 'jdbcStore', 'pluginEnabled', 'purgeQuartzTablesOnStartup'
    ].toSet()

    Environment environment

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!beanFactory.containsBeanDefinition(SCHEDULER_BEAN)) {
            return
        }
        Properties configured = collectQuartzProperties()
        if (configured.isEmpty()) {
            return
        }

        def definition = beanFactory.getBeanDefinition(SCHEDULER_BEAN)
        def existing = definition.propertyValues.getPropertyValue(QUARTZ_PROPERTIES)

        Properties merged = new Properties()
        if (existing?.value instanceof Properties) {
            merged.putAll((Properties) existing.value)
        }
        merged.putAll(configured)
        definition.propertyValues.add(QUARTZ_PROPERTIES, merged)

        log.info("Applied ${configured.size()} quartz setting(s) from configuration: ${configured.stringPropertyNames().sort()}")
    }

    /**
     * Gather {@code quartz.*} from the environment as the {@code org.quartz.*} keys Quartz expects.
     *
     * Property sources are visited in order and the first value for a key wins, so the precedence
     * the environment defines is preserved. Values are read back through the environment so
     * placeholders still resolve.
     *
     * @return the settings to apply, empty when none are configured
     */
    private Properties collectQuartzProperties() {
        Properties collected = new Properties()
        if (!(environment instanceof ConfigurableEnvironment)) {
            return collected
        }
        for (PropertySource<?> source : ((ConfigurableEnvironment) environment).propertySources) {
            if (!(source instanceof EnumerablePropertySource)) {
                continue
            }
            for (String name : ((EnumerablePropertySource) source).propertyNames) {
                if (!name.startsWith(CONFIG_PREFIX)) {
                    continue
                }
                String suffix = name.substring(CONFIG_PREFIX.length())
                if (suffix.isEmpty() || PLUGIN_SWITCHES.contains(suffix)) {
                    continue
                }
                String key = QUARTZ_PREFIX + suffix
                if (collected.containsKey(key)) {
                    continue
                }
                String value = environment.getProperty(name)
                if (value != null) {
                    collected.setProperty(key, value)
                }
            }
        }
        collected
    }
}
