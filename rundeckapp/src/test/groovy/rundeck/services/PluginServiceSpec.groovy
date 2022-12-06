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

package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.AcceptsServices
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.spi.Services
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 6/12/17
 */
class PluginServiceSpec extends Specification implements ServiceUnitTest<PluginService> {
    def "configure plugin does not exist"() {
        given:
        service.rundeckPluginRegistry = Mock(PluginRegistry)
        def name = 'atest'
        def config = [:]
        def project = 'aproject'
        def framework = null
        def providerservice = Mock(PluggableProviderService)

        when:
        def result = service.configurePlugin(name, config, project, framework, providerservice)

        then:
        result == null
        //validatePluginByName returns null
        1 * service.rundeckPluginRegistry.validatePluginByName('atest', providerservice, null, 'aproject', config) >>
                null


    }
    def "configure plugin invalid no errors"() {
        given:
        service.rundeckPluginRegistry = Mock(PluginRegistry)
        def name = 'atest'
        def config = [:]
        def project = 'aproject'
        def framework = null
        def providerservice = Mock(PluggableProviderService)

        when:
        def result = service.configurePlugin(name, config, project, framework, providerservice)

        then:
        result == null
        1 * service.rundeckPluginRegistry.validatePluginByName('atest', providerservice, null, 'aproject', config) >>
                new ValidatedPlugin(valid:false,report: null)


    }

    def "get plugin"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
        when:
            def result = service.getPlugin('blah', String)
        then:
            1 * service.rundeckPluginRegistry.createPluggableService(String) >> providerService
            1 * service.rundeckPluginRegistry.loadPluginByName('blah', providerService) >> 'bloo'
            result == 'bloo'
    }

    def "get plugin descriptor"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
            def describedPlugin = new DescribedPlugin(null, null, 'blah', null, null)
        when:
            def result = service.getPluginDescriptor('blah', ServiceNameConstants.LogFilter)
        then:
            1 * service.rundeckPluginRegistry.createPluggableService(LogFilterPlugin) >> providerService
            1 * service.rundeckPluginRegistry.loadPluginDescriptorByName('blah', providerService) >> describedPlugin
            result == describedPlugin
    }

    def "get plugin type by service"() {
        when:
            def result = service.getPluginTypeByService(svcname)
        then:
            result == clazz
        where:
            svcname                        | clazz
            ServiceNameConstants.LogFilter | LogFilterPlugin
    }

    def "get plugin type by service DNE"() {
        when:
            def result = service.getPluginTypeByService(svcname)
        then:
            IllegalArgumentException e = thrown()
        where:
            svcname      | _
            'DNEService' | _
    }

    def "retain plugin"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
            def closeable = Mock(CloseableProvider)
        when:
            def result = service.retainPlugin('blah', providerService)
        then:
            1 * service.rundeckPluginRegistry.retainPluginByName('blah', providerService) >> closeable
            result == closeable
    }

    def "configure plugin"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
            def configuredPlugin = new ConfiguredPlugin<String>('blah', config)
        when:
            def result = service.configurePlugin(provider, config, String)
        then:
            1 * service.rundeckPluginRegistry.createPluggableService(String) >> providerService
            1 * service.rundeckPluginRegistry.validatePluginByName(provider, providerService, config/*, null*/)
            1 * service.rundeckPluginRegistry.configurePluginByName(provider, providerService, config/*, null*/) >>
            configuredPlugin
            result == configuredPlugin
        where:
            provider    | config
            'aprovider' | [some: 'config']
    }

    @Unroll
    def "getDynamicProperties"() {
        given:
            String project = 'aproject'
            String svcName = 'WorkflowStep'
            String type = 'AProvider'

            def services = Mock(Services)

            def manager = Mock(ProjectManager) {
                getFrameworkProject(project) >> Mock(FrameworkProject) {
                    getProperties() >> projProps
                }
            }

            Properties properties = new Properties()
            properties.putAll(fwkProps)
            def mockSvc = Mock(StepExecutionService)
            def fwk=Mock(IFramework) {
                getFrameworkProjectMgr() >> manager
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)

                getStepExecutionService()>> mockSvc
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
            Description desc = DescriptionBuilder.builder()
                                                 .name(type)
                                                 .property(PropertyBuilder.builder().string('aprop').build())
                                                 .property(PropertyBuilder.builder().string('xprop').build())
                                                 .build()
            def pluginInstance = Mock(DynamicProperties)

            service.rundeckPluginRegistry=Mock(PluginRegistry){
                1 * loadPluginDescriptorByName(type,mockSvc)>>new DescribedPlugin<Object>(pluginInstance, desc, type, null, null)
            }


        when:
            def result = service.getDynamicProperties(fwk,svcName, type, project, services)

        then:
            1 * pluginInstance.dynamicProperties(dynamicInput, services) >> [aprop: ['a', 'b']]
            result == [aprop: ['a', 'b']]

        where:
            fwkProps                                              | projProps | dynamicInput
            [:]                                                   | [:]       | [:]
            ['framework.plugin.WorkflowStep.AProvider.aprop': 'aval'] | [:]       | [aprop: 'aval']
            ['framework.plugin.WorkflowStep.AProvider.aprop': 'aval'] | ['project.plugin.WorkflowStep.AProvider.aprop': 'bval']       | [aprop: 'bval']
            ['framework.plugin.WorkflowStep.AProvider.xprop': 'xval'] | ['project.plugin.WorkflowStep.AProvider.aprop': 'bval']       | [aprop: 'bval',xprop:'xval']
    }


    def "getDynamicProperties with framework mapping"() {
        given:
            String project = 'aproject'
            String svcName = 'WorkflowStep'
            String type = 'AProvider'

            def services = Mock(Services)

            def manager = Mock(ProjectManager) {
                getFrameworkProject(project) >> Mock(FrameworkProject) {
                    getProperties() >> projProps
                }
            }

            Properties properties = new Properties()
            properties.putAll(fwkProps)

            def mockSvc = Mock(StepExecutionService)
            def fwk=Mock(IFramework) {
                getFrameworkProjectMgr() >> manager
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                getStepExecutionService()>> mockSvc
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
            Description desc = DescriptionBuilder.builder()
                                                 .name(type)
                                                 .property(PropertyBuilder.builder().string('aprop').build())
                                                 .property(PropertyBuilder.builder().string('xprop').build())
                                                 .frameworkMapping("custom.aprop","aprop")
                                                 .frameworkMapping("custom.xprop","xprop")
                                                 .build()
            def pluginInstance = Mock(DynamicProperties)
            service.rundeckPluginRegistry=Mock(PluginRegistry){
                1 * loadPluginDescriptorByName(type,mockSvc)>>new DescribedPlugin<Object>(pluginInstance, desc, type, null, null)
            }

        when:
            def result = service.getDynamicProperties(fwk, svcName, type, project, services)

        then:
            1 * pluginInstance.dynamicProperties(dynamicInput, services)>> [aprop: ['a', 'b']]
            result == [aprop: ['a', 'b']]

        where:
            fwkProps                                              | projProps | dynamicInput
            ['custom.aprop': 'aval'] | [:]      | [aprop: 'aval']
            ['custom.xprop': 'xval'] | [:]      | [xprop: 'xval']
            ['custom.aprop': 'aval','custom.xprop': 'xval'] | [:]      | [aprop: 'aval',xprop: 'xval']

    }

    def "getDynamicProperties with project mapping"() {
        given:
            String project = 'aproject'
            String svcName = 'WorkflowStep'
            String type = 'AProvider'

            def services = Mock(Services)

            def manager = Mock(ProjectManager) {
                getFrameworkProject(project) >> Mock(FrameworkProject) {
                    getProperties() >> projProps
                }
            }

            Properties properties = new Properties()
            properties.putAll(fwkProps)
            def mockSvc = Mock(StepExecutionService)

            def fwk=Mock(IFramework) {
                getFrameworkProjectMgr() >> manager
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                getStepExecutionService()>> mockSvc
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
            Description desc = DescriptionBuilder.builder()
                                                 .name(type)
                                                 .property(PropertyBuilder.builder().string('aprop').build())
                                                 .property(PropertyBuilder.builder().string('xprop').build())
                                                 .mapping("custom.aprop","aprop")
                                                 .mapping("custom.xprop","xprop")
                                                 .build()
            def pluginInstance = Mock(DynamicProperties)
            service.rundeckPluginRegistry=Mock(PluginRegistry){
                1 * loadPluginDescriptorByName(type,mockSvc)>>new DescribedPlugin<Object>(pluginInstance, desc, type, null, null)
            }

        when:
            def result = service.getDynamicProperties(fwk, svcName, type, project, services)

        then:
            1 * pluginInstance.dynamicProperties(dynamicInput, services)>> [aprop: ['a', 'b']]
            result == [aprop: ['a', 'b']]

        where:
            fwkProps                                              | projProps | dynamicInput
            [:] | ['custom.aprop': 'aval']      | [aprop: 'aval']
            [:] | ['custom.xprop': 'xval']      | [xprop: 'xval']
            [:] | ['custom.aprop': 'aval','custom.xprop': 'xval']      | [aprop: 'aval',xprop: 'xval']

    }

    @Unroll
    def "getDynamicProperties exception"() {
        given:
        String project = 'aproject'
        String svcName = 'WorkflowStep'
        String type = 'AProvider'

        def services = Mock(Services)

        def manager = Mock(ProjectManager) {
            getFrameworkProject(project) >> Mock(FrameworkProject) {
                getProperties() >> [:]
            }
        }

        Properties properties = new Properties()

        def mockSvc = Mock(StepExecutionService)
        def fwk=Mock(IFramework) {
            getFrameworkProjectMgr() >> manager
            getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever([:])

            getStepExecutionService()>> mockSvc
            getPropertyLookup() >> PropertyLookup.create(properties)
        }
        Description desc = DescriptionBuilder.builder()
                .name(type)
                .property(PropertyBuilder.builder().string('aprop').build())
                .property(PropertyBuilder.builder().string('xprop').build())
                .build()
        def pluginInstance = Mock(DynamicProperties)

        service.rundeckPluginRegistry=Mock(PluginRegistry){
            1 * loadPluginDescriptorByName(type,mockSvc)>>new DescribedPlugin<Object>(pluginInstance, desc, type, null, null)
        }

        when:
        def result = service.getDynamicProperties(fwk,svcName, type, project, services)

        then:
        1 * pluginInstance.dynamicProperties(_, services) >> { throw new Exception() }
        result == null
    }

    def "test shared service api plugin"() {
        given:
        service.rundeckPluginRegistry = Mock(PluginRegistry)
        def providerService = Mock(PluggableProviderService)
        def propertyResolved = Mock(PropertyResolver)
        def servicesProvider = Mock(Services)
        def configuredPlugin = new ConfiguredPlugin<TestNotificationPlugin>(new TestNotificationPlugin(), config)
        def provider = "TestNotificationPlugin"
        def valid = new ValidatedPlugin()
        valid.valid = true

        when:
        def result = service.configurePlugin(provider, providerService, propertyResolved, PropertyScope.Instance, servicesProvider  )
        then:
        1 * service.rundeckPluginRegistry.validatePluginByName(provider, providerService, _, PropertyScope.Instance) >>
                valid
        1 * service.rundeckPluginRegistry.configurePluginByName(provider, providerService,  _, PropertyScope.Instance) >>
                configuredPlugin

        result == configuredPlugin
        result.instance.getService() != null
    }


    class TestNotificationPlugin implements NotificationPlugin, AcceptsServices{

        private Services services
        String passwordStoragePath

        TestNotificationPlugin() {
        }

        void setPasswordStoragePath(String passwordStoragePath) {
            this.passwordStoragePath = passwordStoragePath
        }

        @Override
        void setServices(Services services) {
            this.services = services
        }

        @Override
        boolean postNotification(String trigger, Map executionData, Map config) {
            StorageTree storageTree = this.services.getService(KeyStorageTree)
            def resource = storageTree.getPassword(this.passwordStoragePath)

            if(resource){
                return true
            }

            return false
        }
    }

}
