package com.dtolabs.rundeck.server.plugins


import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class RundeckSpringPluginRegistryComponentSpec extends Specification implements GrailsUnitTest {

    def "register plugin"(){
        given:
            def sut = new RundeckSpringPluginRegistryComponent()
            sut.applicationContext = applicationContext

        when:
            sut.registerPlugin('svc','name','plugin')
        then:
            sut.getPluginRegistryMap()['svc:name'] == 'plugin'

    }

    def "get provider beans"(){
        given:
            def sut = new RundeckSpringPluginRegistryComponent()
            sut.applicationContext = applicationContext
            def obj1 = new Object()
            def obj2 = new Object()
            defineBeans{
                bean1(InstanceFactoryBean, obj1)

                bean2(InstanceFactoryBean, obj2)
            }
            sut.pluginRegistryMap['a']='bean1'
            sut.pluginRegistryMap['b']='bean2'
        when:
            def result=sut.getProviderBeans()
        then:
            result.keySet().size()==2
            result.keySet().containsAll(['bean1','bean2'])
            result.values().containsAll([obj1,obj2])

    }

    def "find provider bean"(){
        given:
            def sut = new RundeckSpringPluginRegistryComponent()
            sut.applicationContext = applicationContext
            def obj1 = new Object()
            defineBeans{
                bean1(InstanceFactoryBean, obj1)

            }
            sut.pluginRegistryMap[reg]='bean1'
        when:
            def result = sut.findProviderBean(type, name)
        then:
            (result==obj1) == found
        where:
            type  | name | reg     | found
            'svc' | 'a' | 'svc:a' | true
            'svc' | 'a' | 'a'     | true
            'svc' | 'a' | 'c'     | false
            'svc' | 'a' | 'svc:c' | false
    }

}
