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

import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.GenericBeanDefinition
import spock.lang.Specification

class InfrastructureRoleBeanDefinitionRegistryPostProcessorSpec extends Specification {

    def processor = new InfrastructureRoleBeanDefinitionRegistryPostProcessor()

    def "DefaultBeanFactoryPointcutAdvisor beans are marked as ROLE_INFRASTRUCTURE"() {
        given:
        def advisorDef = new GenericBeanDefinition(beanClass: DefaultBeanFactoryPointcutAdvisor)
        def registry = Stub(BeanDefinitionRegistry) {
            getBeanDefinitionNames() >> ['org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor#0']
            getBeanDefinition('org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor#0') >> advisorDef
        }

        when:
        processor.postProcessBeanDefinitionRegistry(registry)

        then:
        advisorDef.role == BeanDefinition.ROLE_INFRASTRUCTURE
    }

    def "grailsPromiseFactory bean is marked as ROLE_INFRASTRUCTURE"() {
        given:
        def beanDef = new GenericBeanDefinition()
        def registry = Stub(BeanDefinitionRegistry) {
            getBeanDefinitionNames() >> ['grailsPromiseFactory']
            getBeanDefinition('grailsPromiseFactory') >> beanDef
        }

        when:
        processor.postProcessBeanDefinitionRegistry(registry)

        then:
        beanDef.role == BeanDefinition.ROLE_INFRASTRUCTURE
    }

    def "grailsEventBus bean is marked as ROLE_INFRASTRUCTURE"() {
        given:
        def beanDef = new GenericBeanDefinition()
        def registry = Stub(BeanDefinitionRegistry) {
            getBeanDefinitionNames() >> ['grailsEventBus']
            getBeanDefinition('grailsEventBus') >> beanDef
        }

        when:
        processor.postProcessBeanDefinitionRegistry(registry)

        then:
        beanDef.role == BeanDefinition.ROLE_INFRASTRUCTURE
    }

    def "unrelated beans are not modified"() {
        given:
        def beanDef = new GenericBeanDefinition(beanClass: String)
        def registry = Stub(BeanDefinitionRegistry) {
            getBeanDefinitionNames() >> ['someOtherBean']
            getBeanDefinition('someOtherBean') >> beanDef
        }

        when:
        processor.postProcessBeanDefinitionRegistry(registry)

        then:
        beanDef.role != BeanDefinition.ROLE_INFRASTRUCTURE
    }

    def "postProcessBeanFactory is a no-op"() {
        when:
        processor.postProcessBeanFactory(null)

        then:
        noExceptionThrown()
    }
}
