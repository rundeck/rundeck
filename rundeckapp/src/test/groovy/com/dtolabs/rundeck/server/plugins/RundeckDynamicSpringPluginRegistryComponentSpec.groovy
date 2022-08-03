package com.dtolabs.rundeck.server.plugins

import groovy.transform.CompileStatic
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class RundeckDynamicSpringPluginRegistryComponentSpec extends Specification {

    def "registerDynamicPluginBean"() {
        given:
            def sut = new RundeckDynamicSpringPluginRegistryComponent()
            def context = Stub(ApplicationContext)
        when:
            sut.registerDynamicPluginBean('svc', 'aBean', context)
        then:
            sut.pluginBeanNames == ['svc:aBean': 'aBean']
            sut.subContexts == ['aBean': context]
    }

    def "getProviderBeans"() {
        given:
            def sut = new RundeckDynamicSpringPluginRegistryComponent()
            def obj1 = new Object()
            def obj2 = new Object()
            def context1 = Stub(ApplicationContext) {
                getBean('bean1') >> obj1
            }
            def context2 = Stub(ApplicationContext) {
                getBean('bean2') >> obj2
            }

            sut.pluginBeanNames['a'] = 'bean1'
            sut.subContexts['bean1'] = context1
            sut.pluginBeanNames['b'] = 'bean2'
            sut.subContexts['bean2'] = context2
        when:
            def result = sut.getProviderBeans()
        then:
            result.keySet().size() == 2
            result.keySet().containsAll(['bean1', 'bean2'])
            result.values().containsAll([obj1, obj2])
    }

    def "find provider bean"() {
        given:
            def sut = new RundeckDynamicSpringPluginRegistryComponent()
            def obj1 = new Object()
            def context1 = Stub(ApplicationContext) {
                getBean('bean1') >> obj1
            }

            sut.pluginBeanNames['svc:a'] = 'bean1'
            sut.subContexts['bean1'] = context1
        when:
            def result = sut.findProviderBean(type, name)
        then:
            (result == obj1) == found
        where:
            type  | name |  found
            'svc' | 'a'  |  true
            'svc' | 'b'  |  false
    }

}
