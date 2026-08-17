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
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor

/**
 * Marks the {@code rdAuth*} AOP advisor beans declared in {@code resources.groovy}, along with the
 * Grails async/event-bus infrastructure beans ({@code grailsPromiseFactory}, {@code grailsEventBus}),
 * with {@link BeanDefinition#ROLE_INFRASTRUCTURE}.
 *
 * <p>Without this, Spring's {@code PostProcessorRegistrationDelegate$BeanPostProcessorChecker} logs
 * spurious startup warnings for these beans, e.g.:
 * <pre>
 * Bean 'org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor#0' ... is not eligible for
 * getting processed by all BeanPostProcessors ... Is this bean getting eagerly injected/applied to a
 * currently created BeanPostProcessor [meterRegistryPostProcessor]?
 * </pre>
 * This happens because Spring Boot's method-validation support ({@code methodValidationPostProcessor})
 * and Micrometer's metrics auto-configuration ({@code meterRegistryPostProcessor}) both require an early
 * {@link org.springframework.context.ApplicationContext} reference, forcing them to instantiate before
 * the full BeanPostProcessor chain is registered. Any other bean already registered at that point gets
 * flagged as "not eligible for post-processing" collateral, even though none of the affected beans here
 * (AOP advisor plumbing, Grails' internal async/event-bus factories) are ever expected to be
 * auto-proxied themselves, so marking them as infrastructure-role is safe and does not change runtime
 * behavior — it only suppresses the diagnostic.
 *
 * @see BeanDefinition#ROLE_INFRASTRUCTURE
 */
@Slf4j
@CompileStatic
class InfrastructureRoleBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    /**
     * Bean names, beyond the auto-detected {@link DefaultBeanFactoryPointcutAdvisor} advisors, that
     * should also be marked as infrastructure role. These are Grails plugin-owned beans
     * (grails-async, grails-events) that this application never declares or overrides directly.
     */
    static final List<String> ADDITIONAL_INFRASTRUCTURE_BEAN_NAMES = [
            'grailsPromiseFactory',
            'grailsEventBus',
    ]

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        int markedCount = 0
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition definition = registry.getBeanDefinition(beanName)
            if (definition.beanClassName == DefaultBeanFactoryPointcutAdvisor.name ||
                    beanName in ADDITIONAL_INFRASTRUCTURE_BEAN_NAMES) {
                definition.role = BeanDefinition.ROLE_INFRASTRUCTURE
                markedCount++
                log.debug(
                        "Marked bean [{}] (type [{}]) as ROLE_INFRASTRUCTURE to suppress " +
                                "BeanPostProcessorChecker startup warning",
                        beanName,
                        definition.beanClassName
                )
            }
        }
        log.debug("Marked {} bean definitions as ROLE_INFRASTRUCTURE", markedCount)
    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op: all work is done in postProcessBeanDefinitionRegistry, before bean instantiation begins
    }
}
