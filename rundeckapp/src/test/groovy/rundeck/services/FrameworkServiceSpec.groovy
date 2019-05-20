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

package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import grails.test.mixin.TestFor
import org.rundeck.app.spi.Services
import org.rundeck.core.projects.ProjectConfigurable
import rundeck.PluginStep
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 8/14/15.
 */
@TestFor(FrameworkService)
class FrameworkServiceSpec extends Specification {
    def "summarize tags in nodeset"(){
        given:
        List<INodeEntry> n = nodeList([name:'a',tags:['x','y','z']],
                        [name:'z',tags:['y','a']],
                        [name:'x'])
        when:
        def result=service.summarizeTags(n)

        then:
        n.size()==3
        result == ['x': 1, 'y': 2, 'z': 1, 'a': 1]

    }

    List<INodeEntry> nodeList(Map<String, ?>... maps ) {
        maps.collect{
            def n=new NodeEntryImpl(it.name)
            if(it.tags){
                n.getTags().addAll(it.tags)
            }
            n
        }
    }

    def "get default input charset for project"() {
        given:
        String project = 'aproject'
        def manager = Mock(ProjectManager)
        service.rundeckFramework = Mock(Framework) {
            getFrameworkProjectMgr() >> manager
        }

        when:
        def result = service.getDefaultInputCharsetForProject(project)


        then:
        result == expected
        1 * manager.loadProjectConfig(project) >> Mock(IRundeckProjectConfig) {
            hasProperty(('framework.' + FrameworkService.REMOTE_CHARSET)) >> (fwk ? true : false)
            getProperty(('framework.' + FrameworkService.REMOTE_CHARSET)) >> fwk
            hasProperty(('project.' + FrameworkService.REMOTE_CHARSET)) >> (proj ? true : false)
            getProperty(('project.' + FrameworkService.REMOTE_CHARSET)) >> proj
        }

        where:
        fwk          | proj         | expected     | _
        null         | null         | null         | _
        'UTF-8'      | null         | 'UTF-8'      | _
        null         | 'UTF-8'      | 'UTF-8'      | _
        'UTF-8'      | 'UTF-8'      | 'UTF-8'      | _
        'ISO-8859-2' | 'UTF-8'      | 'UTF-8'      | _
        'UTF-8'      | 'ISO-8859-2' | 'ISO-8859-2' | _


    }

    def "parse plugin config input no desc"() {

        when:
        def result = service.parsePluginConfigInput(null, prefix, params)

        then:
        result == expect

        where:
        prefix | params                   || expect
        ''     | [:]                      || [:]
        ''     | null                     || [:]
        ''     | [a: 'b']                 || [a: 'b']
        ''     | [a: 'b', x: 'y']         || [a: 'b', x: 'y']
        'z.'   | [a: 'b']                 || [:]
        'z.'   | ['z.a': 'b']             || [a: 'b']
        'z.'   | ['z.a': 'b', 'z.x': 'y'] || [a: 'b', x: 'y']
    }

    @Unroll
    def "parse plugin config input with plugin"() {
        given:
        Description desc = DescriptionBuilder.builder()
                                             .name('abc')
                                             .property(PropertyBuilder.builder().string('a').build())
                                             .property(PropertyBuilder.builder().string('x').build())
                                             .build()
        when:
        def result = service.parsePluginConfigInput(desc, prefix, params)

        then:
        result == expect

        where:
        prefix | params                                    || expect
        ''     | [:]                                       || [:]
        ''     | null                                      || [:]
        ''     | [a: 'b']                                  || [a: 'b']
        ''     | [a: 'b', x: 'y']                          || [a: 'b', x: 'y']
        ''     | [a: 'b', x: 'y', nada: 'nothing']         || [a: 'b', x: 'y']
        'z.'   | [a: 'b']                                  || [:]
        'z.'   | ['z.a': 'b']                              || [a: 'b']
        'z.'   | ['z.a': 'b', 'z.x': 'y']                  || [a: 'b', x: 'y']
        'z.'   | ['z.a': 'b', 'z.x': 'y', nada: 'nothing'] || [a: 'b', x: 'y']
    }

    @Unroll
    def "parse plugin config input boolean property value"() {
        given:
        Description desc = DescriptionBuilder.builder()
                                             .name('abc')
                                             .property(PropertyBuilder.builder().booleanType('a').build())
                                             .build()
        when:
        def result = service.parsePluginConfigInput(desc, prefix, [a: avalue])

        then:
        result == [a: expect]

        where:
        avalue         || expect
        'true'         || 'true'
        'on'           || 'true'
        'b'            || 'false'
        'anythingelse' || 'false'
        prefix = ''
    }

    static class TestConfigurableBean implements ProjectConfigurable {

        Map<String, String> categories = [:]

        List<Property> projectConfigProperties = []

        Map<String, String> propertiesMapping = [:]
    }

    def "loadProjectConfigurableInput"() {

        given:

        defineBeans {
            testConfigurableBean(TestConfigurableBean) {
                projectConfigProperties = ScheduledExecutionService.ProjectConfigProperties
                propertiesMapping = ScheduledExecutionService.ConfigPropertiesMapping
                categories = [groupExpandLevel: 'gui', disableExecution: 'executionMode', disableSchedule: 'executionMode',]
            }
        }
        String prefix = 'extraConfig.'
        def category = null
        service.applicationContext = applicationContext
        when:
        def result = service.loadProjectConfigurableInput(prefix, projProps, category)

        then:
        result['testConfigurableBean'] != null
        result['testConfigurableBean'].name == 'testConfigurableBean'
        result['testConfigurableBean'].configurable != null
        result['testConfigurableBean'].values == expect

        where:
        projProps                                                                    | expect
        [:]                                                                          | [:]
        ['project.disable.executions': 'false', 'project.disable.schedule': 'false'] | [disableExecution: 'false',
                                                                                        disableSchedule: 'false']
        ['project.disable.executions': 'true', 'project.disable.schedule': 'false']  | [disableExecution: 'true',
                                                                                        disableSchedule: 'false']
        ['project.disable.executions': 'false', 'project.disable.schedule': 'true']  | [disableExecution: 'false',
                                                                                        disableSchedule: 'true']
        ['project.disable.executions': 'false', 'project.disable.schedule': 'true']  | [disableExecution: 'false',
                                                                                        disableSchedule: 'true']
        ['project.jobs.gui.groupExpandLevel': '3']                                   | [groupExpandLevel: '3']
    }

    @Unroll
    def "validateProjectConfigurableInput"() {

        given:

        defineBeans {
            testConfigurableBean(TestConfigurableBean) {
                projectConfigProperties = ScheduledExecutionService.ProjectConfigProperties
                propertiesMapping = ScheduledExecutionService.ConfigPropertiesMapping
                categories = [groupExpandLevel: 'gui', disableExecution: 'executionMode', disableSchedule: 'executionMode',]
            }
        }
        String prefix = 'extraConfig.'
        def category = null
        service.applicationContext = applicationContext
        when:
        def result = service.validateProjectConfigurableInput([testConfigurableBean: input], prefix, category)

        then:

        result.errors == []
        result.config['testConfigurableBean'].name == 'testConfigurableBean'
        result.config['testConfigurableBean'].configurable != null
        result.config['testConfigurableBean'].prefix == prefix + 'testConfigurableBean.'
        result.config['testConfigurableBean'].values == values
        result.config['testConfigurableBean'].report != null
        result.config['testConfigurableBean'].report.valid
        result.props == expect
        result.remove == ScheduledExecutionService.ConfigPropertiesMapping.values().toList()
        where:
        input                       | values|expect
        [:]                         | [disableExecution:'false', disableSchedule:'false']|['project.disable.executions': 'false','project.disable.schedule': 'false']
        [groupExpandLevel: '2']     | [groupExpandLevel:'2',disableExecution:'false', disableSchedule:'false']|['project.disable.executions': 'false','project.disable.schedule': 'false','project.jobs.gui.groupExpandLevel': '2']
        [disableExecution: 'false'] | [disableExecution:'false', disableSchedule:'false']|['project.disable.executions': 'false','project.disable.schedule': 'false']
        [disableExecution: 'blah']  | [disableExecution:'false', disableSchedule:'false']|['project.disable.executions': 'false','project.disable.schedule': 'false']
        [disableExecution: 'true']  | [disableExecution:'true', disableSchedule:'false']|['project.disable.executions': 'true','project.disable.schedule': 'false']
    }

    def "getDynamicProperties"() {
        given:
            String project = 'aproject'
            String svcName = 'AService'
            String type = 'AProvider'

            def services = Mock(Services)

            def manager = Mock(ProjectManager) {
                getFrameworkProject(project) >> Mock(FrameworkProject) {
                    getProperties() >> projProps
                }
            }

            service.rundeckFramework = Mock(Framework) {
                getFrameworkProjectMgr() >> manager
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
            }
            Description desc = DescriptionBuilder.builder()
                                                 .name(type)
                                                 .property(PropertyBuilder.builder().string('aprop').build())
                                                 .property(PropertyBuilder.builder().string('xprop').build())
                                                 .build()
            def pluginInstance = Mock(DynamicProperties)
            service.pluginService = Mock(PluginService) {
                getPluginDescriptor(type, svcName) >> new DescribedPlugin<Object>(pluginInstance, desc, type)
            }


        when:
            def result = service.getDynamicProperties(svcName, type, project, services)

        then:
            1 * pluginInstance.dynamicProperties(dynamicInput, services) >> [aprop: ['a', 'b']]
            result == [aprop: ['a', 'b']]

        where:
            fwkProps                                              | projProps | dynamicInput
            [:]                                                   | [:]       | [:]
            ['framework.plugin.AService.AProvider.aprop': 'aval'] | [:]       | [aprop: 'aval']
            ['framework.plugin.AService.AProvider.aprop': 'aval'] | ['project.plugin.AService.AProvider.aprop': 'bval']       | [aprop: 'bval']
            ['framework.plugin.AService.AProvider.xprop': 'xval'] | ['project.plugin.AService.AProvider.aprop': 'bval']       | [aprop: 'bval',xprop:'xval']
    }

    def "getServicePropertiesMapForType missing provider"() {
        given:
            service.pluginService = Mock(PluginService)
            def type = 'atype'
            def svc = null
            def props = [:]
        when:
            def result = service.getServicePropertiesMapForType(type, svc, props)
        then:
            1 * service.pluginService.getPluginDescriptor(type, svc) >> null
            result == [:]
    }

    def "addProjectNodeExecutorPropertiesForType missing provider"() {
        given:
            def type = 'atype'
            Properties props = [:]
            def config = [:]
            def remove = ['blah'].toSet()
            service.pluginService = Mock(PluginService)
            service.initialized = true
            service.rundeckFramework = Mock(Framework) {

            }
        when:
            service."$method"(type, props, config, remove)
        then:
            1 * service.pluginService.getPluginDescriptor(type, _) >> null
            props == [:]
            config == [:]

        where:
            method << ['addProjectNodeExecutorPropertiesForType', 'addProjectFileCopierPropertiesForType']
    }


    def "getFirstLoginFile"() {
        when:
        File tmpVar = File.createTempDir()
        service.rundeckFramework = Mock(Framework) {
            hasProperty('framework.var.dir') >> { true }
            getProperty('framework.var.dir') >> { tmpVar }
        }
        File firstLoginMarker = service.getFirstLoginFile()

        then:
        !firstLoginMarker.exists()
        firstLoginMarker.name == FrameworkService.FIRST_LOGIN_FILE
        firstLoginMarker.absolutePath == tmpVar.absolutePath+"/"+FrameworkService.FIRST_LOGIN_FILE

    }

    def "getFirstLoginFile no framework var dir"() {
        when:
        File tmpVar = File.createTempDir()
        service.rundeckFramework = Mock(Framework) {
            hasProperty('framework.var.dir') >> { false }
            getBaseDir() >> { tmpVar }
        }
        File firstLoginMarker = service.getFirstLoginFile()

        then:
        !firstLoginMarker.exists()
        firstLoginMarker.name == FrameworkService.FIRST_LOGIN_FILE
        firstLoginMarker.absolutePath == tmpVar.absolutePath+"/var/"+FrameworkService.FIRST_LOGIN_FILE

    }
  
    @Unroll
    def "discoverScopedConfiguration"() {
        given:
            service.pluginService = Mock(PluginService)
        when:
            def result = service.discoverScopedConfiguration(props as Properties, prefix)

        then:
            count * service.pluginService.hasPluginService(_) >> {
                types.contains(it[0])
            }
            service.pluginService.listPluginDescriptions('svc')>>[
                    DescriptionBuilder.builder().name('type1').property(PropertyBuilder.builder().string('p1')).build(),
                    DescriptionBuilder.builder().name('type2').property(PropertyBuilder.builder().string('p2')).build(),
            ]
            service.pluginService.listPluginDescriptions('svc2')>>[
                    DescriptionBuilder.builder().name('typeA').property(PropertyBuilder.builder().string('pA')).build(),
                    DescriptionBuilder.builder().name('typeB').property(PropertyBuilder.builder().string('pB')).build(),
            ]
            result == expect

        where:
            props                         | prefix | count | expect           | types
            [:]                           | 'a'    | 0     | [:]              | []
            ['a.svc.type1.p1': 'v'] | 'a'    | 1     | [svc: [type1:[p1:'v']]] | ['svc']
            ['a.svc.type1.p1': 'v','a.svc.type2.p2': 'd'] | 'a'    | 1     | [svc: [type1:[p1:'v'],type2:[p2:'d']]] | ['svc']
            ['a.svc.type1.p1': 'v','a.svc2.typeA.pA': 'q'] | 'a'    | 2     | [svc: [type1:[p1:'v']],svc2:[typeA:[pA:'q']]] | ['svc','svc2']
            ['a.svc.type1.p1': 'v','a.svc2.typeA.pA': 'q'] | 'a'    | 2     | [svc2:[typeA:[pA:'q']]] | ['svc2']
            ['a.svc.type1.p1': 'v','a.svc2.typeA.pA': 'q'] | 'a'    | 2     | [svc: [type1:[p1:'v']]] | ['svc']
    }
    @Unroll
    def "list scoped service providers"() {
        given:
            service.pluginService = Mock(PluginService)
        when:
            def result = service.listScopedServiceProviders(props as Properties, prefix)

        then:
            count * service.pluginService.hasPluginService(_) >> {
                types.contains(it[0])
            }
            service.pluginService.listPluginDescriptions('svc')>>[
                    DescriptionBuilder.builder().name('type1').property(PropertyBuilder.builder().string('p1')).build(),
                    DescriptionBuilder.builder().name('type2').property(PropertyBuilder.builder().string('p2')).build(),
            ]
            service.pluginService.listPluginDescriptions('svc2')>>[
                    DescriptionBuilder.builder().name('typeA').property(PropertyBuilder.builder().string('pA')).build(),
                    DescriptionBuilder.builder().name('typeB').property(PropertyBuilder.builder().string('pB')).build(),
            ]
            result.keySet() == expect.keySet()
            expect['svc']?.toSet() == (result['svc']*.name)?.toSet()
            expect['svc2']?.toSet() == (result['svc2']*.name)?.toSet()

        where:
            props                         | prefix | count | expect           | types
            [:]                           | 'a'    | 0     | [:]              | []
            ['a.svc.type1.p1': 'v'] | 'a'    | 1     | [svc: ['type1']] | ['svc']
            ['a.svc.type1.p1': 'v','a.svc.type2.p2': 'd'] | 'a'    | 1     | [svc: ['type1','type2']] | ['svc']
            ['a.svc.type1.p1': 'v','a.svc2.typeA.pA': 'q'] | 'a'    | 2     | [svc: ['type1'],svc2:['typeA']] | ['svc','svc2']
            ['a.svc.type1.p1': 'v','a.svc2.typeA.pA': 'q'] | 'a'    | 2     | [svc2:['typeA']] | ['svc2']
            ['a.svc.type1.p1': 'v','a.svc2.typeA.pA': 'q'] | 'a'    | 2     | [svc: ['type1']] | ['svc']
    }

    def "get plugin control service"() {
        given:
            service.rundeckFramework = Mock(Framework)

            def ctrla = service.getPluginControlService('projectA')
            def ctrlb = service.getPluginControlService('projectB')
        when:
            def pluga = ctrla.listDisabledPlugins()
            def plugb = ctrlb.listDisabledPlugins()
        then:
            ctrla != ctrlb
            pluga != plugb
            2 * service.rundeckFramework.getFrameworkProjectMgr() >> Mock(ProjectManager) {
                1 * getFrameworkProject('projectA') >> Mock(IRundeckProject) {
                    1 * hasProperty('disabled.plugins') >> true
                    1 * getProperty('disabled.plugins') >> 'a,b,c'
                }
                1 * getFrameworkProject('projectB') >> Mock(IRundeckProject) {
                    hasProperty('disabled.plugins') >> false

                }
            }
            pluga == ['a', 'b', 'c']
            plugb == []
    }

    @Unroll
    def "get plugin description for step item nodeStep #nodeStep description #descriptor"() {
        given:
            service.rundeckFramework = Mock(Framework)
            service.pluginService = Mock(PluginService)
            def step = new PluginStep(type: 'atype', nodeStep: nodeStep)
            def nodeStepService = Mock(NodeStepExecutionService)
            def wfStepService = Mock(StepExecutionService)
            def desc = DescriptionBuilder.builder().name('atype').build()
        when:
            def result = service.getPluginDescriptionForItem(step)
        then:
            if (nodeStep) {
                1 * service.rundeckFramework.getNodeStepExecutorService() >> nodeStepService
                1 * service.pluginService.getPluginDescriptor('atype', nodeStepService) >>
                (descriptor ? new DescribedPlugin(_, desc, 'atype') : null)
            } else {
                1 * service.rundeckFramework.getStepExecutionService() >> wfStepService
                1 * service.pluginService.getPluginDescriptor('atype', wfStepService) >>
                (descriptor ? new DescribedPlugin(_, desc, 'atype') : null)
            }
            result == (descriptor ? desc : null)

        where:
            nodeStep | descriptor
            true     | true
            true     | false
            false    | true
            false    | false
    }
}
