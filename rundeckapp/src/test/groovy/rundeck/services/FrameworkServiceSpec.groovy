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

import com.dtolabs.rundeck.app.support.ExecutionCleanerConfig
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.AclRuleBuilder
import com.dtolabs.rundeck.core.authorization.AclRuleImpl
import com.dtolabs.rundeck.core.authorization.AclRuleSet
import com.dtolabs.rundeck.core.authorization.AclRuleSetAuthorization
import com.dtolabs.rundeck.core.authorization.AclRuleSetImpl
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import grails.events.bus.EventBus
import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.spi.Services
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.projects.ProjectConfigurable
import rundeck.PluginStep
import rundeck.services.feature.FeatureService
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject
import javax.servlet.http.HttpSession

/**
 * Created by greg on 8/14/15.
 */
class FrameworkServiceSpec extends Specification implements ServiceUnitTest<FrameworkService> {
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

    def "analyze properties change"(){
        setup:
        def project = 'test'
        def sEService=Mock(MockScheduledExecutionService)
        [
                rescheduleJobs:{a,b->
                },
                unscheduleJobsForProject:{a,b->
                }

        ]
        def properties = ['uuid': System.getProperty("rundeck.server.uuid"),
                          'props':['project': project, 'projSchedExecProps':
                                      ['isEnabled': (!disableSchedule && !disableExecution),
                                       'oldDisableEx': currentExecutionDisabled, 'oldDisableSched': currentScheduleDisabled]]]

        service.scheduledExecutionService = sEService
        service.grailsEventBus = Mock(EventBus)
        service.configurationService=Mock(ConfigurationService)
        when:
        service.handleProjectSchedulingEnabledChange(project, currentExecutionDisabled, currentScheduleDisabled,
                disableExecution, disableSchedule)

        then:
        if(shouldReSchedule){
            1 * sEService.rescheduleJobs(_,_)
        }else{
            0 * sEService.rescheduleJobs(_,_)
        }
        if(shouldUnSchedule){
            1 * sEService.unscheduleJobsForProject(_,_)
        }else{
            0 * sEService.unscheduleJobsForProject(_,_)
        }
        if(shouldUnSchedule || shouldReSchedule)
            1 * service.grailsEventBus.notify('project.scheduling.changed',[properties])


        where:
        currentExecutionDisabled | currentScheduleDisabled | disableExecution | disableSchedule | shouldReSchedule | shouldUnSchedule
        false                    | false                   | false            | false           | false            | false
        false                    | false                   | true             | false           | false            | true
        false                    | false                   | false            | true            | false            | true
        false                    | false                   | true             | true            | false            | true
        true                     | false                   | false            | false           | true             | false
        true                     | false                   | true             | false           | false            | false
        true                     | false                   | false            | true            | false            | true
        true                     | false                   | true             | true            | false            | true
        false                    | true                    | false            | false           | true             | false
        false                    | true                    | true             | false           | false            | true
        false                    | true                    | false            | true            | false            | false
        false                    | true                    | true             | true            | false            | true
        true                     | true                    | false            | false           | true             | false
        true                     | true                    | true             | false           | false            | true
        true                     | true                    | false            | true            | false            | true
        true                     | true                    | true             | true            | false            | false
    }

    @Unroll
    def "validateProjectConfigurableInput check default values"() {

        given:

        defineBeans {
            testConfigurableBean(TestConfigurableBean) {
                projectConfigProperties =  [
                        PropertyBuilder.builder().with {
                            booleanType 'enabled'
                            title 'Health Checks Enabled'
                            description ''
                            required(false)
                            defaultValue 'true'
                        }.build(),
                        PropertyBuilder.builder().with {
                            booleanType 'onstartup'
                            title 'Initiate Health Checks on Startup'
                            description 'the server starts.'
                            required(false)
                            defaultValue 'true'
                        }.build()

                ]
                propertiesMapping = ['enabled': 'project.healthcheck.enabled','onstartup': 'project.healthcheck.onstartup']
                categories = [enabled: 'resourceModelSource', onstartup: 'resourceModelSource']
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
        where:
        input                 | values|expect
        [:]                   | [enabled:'true', onstartup:'true']|['project.healthcheck.enabled': 'true','project.healthcheck.onstartup': 'true']
        [enabled: 'false']    | [enabled:'false',onstartup:'true']|['project.healthcheck.enabled': 'false','project.healthcheck.onstartup': 'true']
        [onstartup: 'false']  | [enabled:'true',onstartup:'false']|['project.healthcheck.enabled': 'true','project.healthcheck.onstartup': 'false']
        [enabled: 'false']    | [enabled:'false',onstartup:'true']|['project.healthcheck.enabled': 'false','project.healthcheck.onstartup': 'true']

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
            1 * service.pluginService.getPluginDescriptor(type, svc) >> retval
            result == [:]
        where:
            retval                                   | _
            null                                     | _
            new DescribedPlugin(null, null, 'atype', null, null) | _
    }

    def "remapReportProperties missing provider"() {
        given:
            service.pluginService = Mock(PluginService)
            def type = 'atype'
            def svc = Mock(PluggableProviderRegistryService)
        when:
            def result = service.remapReportProperties(null, type, svc)
        then:
            1 * service.pluginService.getPluginDescriptor(type, svc) >> retval
            result != null
            result.errors == [:]

        where:
            retval                                   | _
            null                                     | _
            new DescribedPlugin(null, null, 'atype', null, null) | _
    }

    def "addProjectNodeExecutorPropertiesForType missing provider"() {
        given:
            def type = 'atype'
            Properties props = [:]
            def config = [:]
            def remove = ['blah'].toSet()
            service.pluginService = Mock(PluginService)
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
        firstLoginMarker.absolutePath == tmpVar.absolutePath + File.separator +FrameworkService.FIRST_LOGIN_FILE

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
        firstLoginMarker.absolutePath == tmpVar.absolutePath+File.separator + "var" + File.separator +FrameworkService.FIRST_LOGIN_FILE

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
            service.rundeckFramework = Mock(Framework){
                getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    1 * getFrameworkProject(project) >> Mock(IRundeckProject) {
                        1 * hasProperty('disabled.plugins') >> hasProp
                        getProperty('disabled.plugins') >> propVal
                    }
                }
            }

            def ctrla = service.getPluginControlService(project)
        when:
            def pluga = ctrla.listDisabledPlugins()
        then:

            pluga == expect
        where:
            project    | hasProp | propVal | expect
            'projectA' | true    | 'a,b,c' | ['a', 'b', 'c']
            'projectB' | false   | null    | []
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
                (descriptor ? new DescribedPlugin(_, desc, 'atype', null, null) : null)
            } else {
                1 * service.rundeckFramework.getStepExecutionService() >> wfStepService
                1 * service.pluginService.getPluginDescriptor('atype', wfStepService) >>
                (descriptor ? new DescribedPlugin(_, desc, 'atype', null, null) : null)
            }
            result == (descriptor ? desc : null)

        where:
            nodeStep | descriptor
            true     | true
            true     | false
            false    | true
            false    | false
    }

    def "refresh session projects"() {
        given:
            def auth = Mock(UserAndRolesAuthContext)
            def session = [:]
            service.metricService=Mock(MetricService){
                withTimer(_,_,_)>>{
                    it[2].call()
                }
            }
            service.rundeckFramework = Mock(Framework) {
                getFrameworkProjectMgr() >> Stub(ProjectManager) {
                    listFrameworkProjectNames() >> names
                    getFrameworkProject(_) >> {
                        def name = it[0]
                        return Mock(IRundeckProject) {
                            getProperty('project.label') >> (name + ' Label')
                            hasProperty('project.label') >> true
                        }
                    }
                }
            }
            service.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator) {
                authorizeApplicationResourceAny(auth, _, _) >> {
                    return it[1].name in authed
                }
                authResourceForProject(_)>>{
                    return [name:(it[0])]
                }
            }
            service.configurationService=Mock(ConfigurationService){
                1 * getLong('userSessionProjectsCache.refreshDelay', 5 * 60 * 1000L)>>{
                    it[1]
                }
            }
            service.featureService=Mock(FeatureService){
                1 * featurePresent(Features.SIDEBAR_PROJECT_LISTING)>>true
            }
        when:
            def result = service.refreshSessionProjects(auth, session)
        then:
            result == sortedList
            session.frameworkProjects == sortedList
            session.frameworkLabels == labels
        where:
            names           | authed          | sortedList      | labels
            ['z', 'y', 'x'] | ['z', 'y', 'x'] | ['x', 'y', 'z'] | [z: 'z Label', x: 'x Label', y: 'y Label']
            ['z', 'y', 'x'] | ['z', 'y',]     | ['y', 'z']      | [z: 'z Label', y: 'y Label']

    }
    def "refresh session projects disable feature sidebarProjectListing does not load labels"() {
            def auth = Mock(UserAndRolesAuthContext)
            def session = [:]
            service.metricService=Mock(MetricService){
                withTimer(_,_,_)>>{
                    it[2].call()
                }
            }
            def projectMgr = Mock(ProjectManager) {
                listFrameworkProjectNames() >> names
            }
            service.rundeckFramework = Mock(Framework) {
                getFrameworkProjectMgr() >> projectMgr
            }
            service.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator) {
                authorizeApplicationResourceAny(auth, _, _) >> {
                    return it[1].name in authed
                }
                authResourceForProject(_)>>{
                    return [name:(it[0])]
                }
            }
            service.configurationService=Mock(ConfigurationService){
                1 * getLong('userSessionProjectsCache.refreshDelay', 5 * 60 * 1000L)>>{
                    it[1]
                }
            }
        given: "sidebar project listing feature disabled"
            service.featureService=Mock(FeatureService){
                1 * featurePresent(Features.SIDEBAR_PROJECT_LISTING)>>false
            }
        when:
            def result = service.refreshSessionProjects(auth, session)
        then:
            result == sortedList
            session.frameworkProjects == sortedList
            session.frameworkLabels == [:]
            0 * projectMgr.getFrameworkProject(_)
        where:
            names           | authed          | sortedList      | labels
            ['z', 'y', 'x'] | ['z', 'y', 'x'] | ['x', 'y', 'z'] | [z: 'z Label', x: 'x Label', y: 'y Label']
            ['z', 'y', 'x'] | ['z', 'y',]     | ['y', 'z']      | [z: 'z Label', y: 'y Label']

    }

    def "projectLabels method reads labels"() {
        given:
            def projectMgr = Mock(ProjectManager) {
                3 * getFrameworkProject(_) >> {
                    def name=it[0]
                    Mock(IRundeckProject) {
                        getProperty("project.label") >> (name+' Label')
                    }
                }
            }
            service.rundeckFramework = Mock(Framework) {
                getFrameworkProjectMgr() >> projectMgr
            }
        when:
            def result = service.projectLabels(names)
        then:
            result == labels
        where:
            names           | labels
            ['z', 'y', 'x'] | [z: 'z Label', x: 'x Label', y: 'y Label']

    }
    @Unroll
    def "loadSessionProjectLabel updates one project label"() {
        given:
            def projectMgr = Mock(ProjectManager) {
                (newLabel?0:1) * getFrameworkProject('aproject') >> {
                    Stub(IRundeckProject) {
                        getProperty("project.label") >> 'loaded label'
                    }
                }
            }
            service.rundeckFramework = Stub(Framework) {
                getFrameworkProjectMgr() >> projectMgr
            }
            def labels=[:]
            def session = Mock(HttpSession){
                getAttribute('frameworkLabels')>>labels
            }
        when:
            def result = service.loadSessionProjectLabel(session,'aproject', newLabel)
        then:
            result == expect
            labels == [aproject: expect]
        where:
            newLabel    | expect
            null        | 'loaded label'
            'set label' | 'set label'
    }
    def "refresh session projects fills cache with feature flag"() {
            def auth = Mock(UserAndRolesAuthContext)
            def session = [:]
            service.metricService=Mock(MetricService){
                withTimer(_,_,_)>>{
                    it[2].call()
                }
            }
            service.rundeckFramework = Mock(Framework) {
                getFrameworkProjectMgr() >> Stub(ProjectManager) {
                    listFrameworkProjectNames() >> names
                    getFrameworkProject(_) >> {
                        def name = it[0]
                        return Mock(IRundeckProject) {
                            getProperty('project.label') >> (name + ' Label')
                            hasProperty('project.label') >> true
                        }
                    }
                }
            }
            service.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator) {
                authorizeApplicationResourceAny(auth, _, _) >> {
                    return it[1].name in authed
                }
                authResourceForProject(_)>>{
                    return [name:(it[0])]
                }
            }
            service.configurationService=Mock(ConfigurationService){
                1 * getLong('userSessionProjectsCache.refreshDelay', 5 * 60 * 1000L)>>{
                    it[1]
                }
            }
        given: "user session projects cache feature enabled"
            service.featureService=Mock(FeatureService){
                1 * featurePresent(Features.SIDEBAR_PROJECT_LISTING)>>true
                1 * featurePresent(Features.USER_SESSION_PROJECTS_CACHE)>>true
            }
        when:
            def result = service.refreshSessionProjects(auth, session)
        then:
            result == sortedList
            session.frameworkProjects == sortedList
            session.frameworkLabels == labels
            session.frameworkProjects_expire > System.currentTimeMillis()
        where:
            names           | authed          | sortedList      | labels
            ['z', 'y', 'x'] | ['z', 'y', 'x'] | ['x', 'y', 'z'] | [z: 'z Label', x: 'x Label', y: 'y Label']
            ['z', 'y', 'x'] | ['z', 'y',]     | ['y', 'z']      | [z: 'z Label', y: 'y Label']

    }

    def "scheduleCleanerExecutions not enabled"() {
        given:
            def project = 'AProject'
            def config = Mock(ExecutionCleanerConfig)
            service.scheduledExecutionService = Mock(ScheduledExecutionService)
        when:
            service.scheduleCleanerExecutions(project, config)
        then:
            1 * service.scheduledExecutionService.deleteCleanerExecutionsJob(project)
            0 * service.scheduledExecutionService.scheduleCleanerExecutionsJob(project, _, _)
    }

    def "scheduleCleanerExecutions enabled"() {
        given:
            def project = 'AProject'
            def config = Mock(ExecutionCleanerConfig) {
                isEnabled() >> true
                getCronExpression()>>'cron1'
                getMaxDaysToKeep()>>1
                getMaximumDeletionSize()>>2
                getMinimumExecutionToKeep()>>3
            }
            service.scheduledExecutionService = Mock(ScheduledExecutionService)
        when:
            service.scheduleCleanerExecutions(project, config)
        then:
            1 * service.scheduledExecutionService.deleteCleanerExecutionsJob(project)
            1 * service.scheduledExecutionService.scheduleCleanerExecutionsJob(project, 'cron1', { it.maxDaysToKeep==1 && it.maximumDeletionSize==2 && it.minimumExecutionToKeep==3 })
    }
    def "getProjectCleanerExecutionsScheduledConfig"(){
        given:
            def project='ProjectA'
            def manager = Mock(ProjectManager)
            service.rundeckFramework = Mock(Framework) {
                getFrameworkProjectMgr() >> manager
            }
            1 * manager.getFrameworkProject(project)>>Mock(IRundeckProject){
                getProjectProperties() >> [
                    'project.execution.history.cleanup.enabled'          : enabled,
                    'project.execution.history.cleanup.schedule'         : 'cron1',
                    'project.execution.history.cleanup.retention.days'   : days,
                    'project.execution.history.cleanup.retention.minimum': min,
                    'project.execution.history.cleanup.batch'            : batch,
                ]
            }
        when:
            def result = service.getProjectCleanerExecutionsScheduledConfig(project)
        then:
            result.enabled==expectEnabled
            result.cronExpression == 'cron1'
            result.maxDaysToKeep == expectDays
            result.minimumExecutionToKeep == expectMin
            result.maximumDeletionSize == expectBatch
        where:
            enabled | expectEnabled | days   | expectDays | min    | expectMin | batch  | expectBatch
            'true'  | true          | '1'    | 1          | '2'    | 2         | '3'    | 3
            'true'  | true          | '1'    | 1          | '2'    | 2         | null   | 500
            'true'  | true          | '1'    | 1          | '2'    | 2         | 'asdf' | 500
            'true'  | true          | '1'    | 1          | null   | 0         | null   | 500
            'true'  | true          | '1'    | 1          | 'asdf' | 0         | null   | 500
            'true'  | true          | null   | -1         | null   | 0         | null   | 500
            'true'  | true          | 'asdf' | -1         | null   | 0         | null   | 500
            null    | false         | null   | -1         | null   | 0         | null   | 500
            'false' | false         | null   | -1         | null   | 0         | null   | 500

    }
    def "refresh session projects uses cache with feature flag"() {
            def auth = Mock(UserAndRolesAuthContext)
            def session = [:]
            service.metricService=Mock(MetricService){
                withTimer(_,_,_)>>{
                    it[2].call()
                }
            }
            def projectMgr = Mock(ProjectManager)
            service.rundeckFramework = Mock(Framework) {
                getFrameworkProjectMgr() >> projectMgr
            }
            service.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator)
            service.configurationService=Mock(ConfigurationService){
                1 * getLong('userSessionProjectsCache.refreshDelay', 5 * 60 * 1000L)>>{
                    it[1]
                }
            }
        given: "user session projects cache feature enabled"
            service.featureService=Mock(FeatureService){
                1 * featurePresent(Features.USER_SESSION_PROJECTS_CACHE)>>true
            }
            session.frameworkProjects_expire = System.currentTimeMillis() + (1000*1000L)
            session.frameworkProjects=sortedList
            session.frameworkProjects_count=sortedList.size()
            session.frameworkLabels=labels
        when:
            def result = service.refreshSessionProjects(auth, session)
        then:
            1 * projectMgr.countFrameworkProjects() >> authed.size()
            0 * projectMgr.listFrameworkProjectNames()
            0 * service.rundeckAuthContextEvaluator.authorizeApplicationResourceSet(*_)
            result == sortedList
            session.frameworkProjects == sortedList
            session.frameworkLabels == labels
            session.frameworkProjects_expire > 0
        where:
            names           | authed          | sortedList      | labels
            ['z', 'y', 'x'] | ['z', 'y', 'x'] | ['x', 'y', 'z'] | [z: 'z Label', x: 'x Label', y: 'y Label']
    }
    def "refresh session projects resets cache when project count changes"() {
            def auth = Mock(UserAndRolesAuthContext)
            def session = [:]
            service.metricService=Mock(MetricService){
                withTimer(_,_,_)>>{
                    it[2].call()
                }
            }
            def projectMgr = Mock(ProjectManager) {
                1 * listFrameworkProjectNames() >> names
                1 * countFrameworkProjects() >> authed.size()
                _ * getFrameworkProject(_) >> {
                    def name = it[0]
                    return Mock(IRundeckProject) {
                        getProperty('project.label') >> (name + ' Label')
                        hasProperty('project.label') >> true
                    }
                }
            }
            service.rundeckFramework = Stub(Framework) {
                getFrameworkProjectMgr() >> projectMgr
            }
            service.configurationService=Mock(ConfigurationService){
                1 * getLong('userSessionProjectsCache.refreshDelay', 5 * 60 * 1000L)>>{
                    it[1]
                }
            }
        given: "user session projects cache feature enabled"
            service.featureService=Mock(FeatureService){
                1 * featurePresent(Features.USER_SESSION_PROJECTS_CACHE)>>true
                1 * featurePresent(Features.SIDEBAR_PROJECT_LISTING)>>true
            }
            service.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator) {
                authorizeApplicationResourceAny(auth, _, _) >> {
                    return it[1].name in authed
                }
                authResourceForProject(_)>>{
                    return [name:(it[0])]
                }
            }
            session.frameworkProjects_expire = System.currentTimeMillis() + (1000 * 1000L)
            session.frameworkProjects = sortedList
            session.frameworkProjects_count = (sortedList.size() - 1)
            session.frameworkLabels = labels
        when:
            def result = service.refreshSessionProjects(auth, session)
        then:
            result == sortedList
            session.frameworkProjects == sortedList
            session.frameworkLabels == labels
            session.frameworkProjects_expire > 0
        where:
            names           | authed          | sortedList      | labels
            ['z', 'y', 'x'] | ['z', 'y', 'x'] | ['x', 'y', 'z'] | [z: 'z Label', x: 'x Label', y: 'y Label']
    }

    def "authorized projectNames"(){
        given:
            def auth = Mock(UserAndRolesAuthContext)
            def projectMgr = Mock(ProjectManager) {
                1 * listFrameworkProjectNames() >> names
            }
            service.rundeckFramework = Stub(Framework) {
                getFrameworkProjectMgr() >> projectMgr
            }
            service.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator) {
                authorizeApplicationResourceAny(auth, _, [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> {
                    return it[1].name in authed
                }
                authResourceForProject(_)>>{
                    return [name:(it[0])]
                }
            }
        when:
            def result=service.projectNames(auth)
        then:
            result == sortedList
        where:
            names           | authed          | sortedList
            ['z', 'y', 'x'] | ['z', 'y', 'x'] | ['x', 'y', 'z']
            ['z', 'y', 'x'] | ['z',]          | ['z']
    }
    def "authorized projects"(){
        given:
            def auth = Mock(UserAndRolesAuthContext)
            def projectMgr = Mock(ProjectManager) {
                1 * listFrameworkProjectNames() >> names
                _ * getFrameworkProject(_) >> {
                    def name = it[0]
                    return Mock(IRundeckProject) {
                        getProperty('project.label') >> (name + ' Label')
                        hasProperty('project.label') >> true
                        getName()>>name
                    }
                }
            }
            service.rundeckFramework = Stub(Framework) {
                getFrameworkProjectMgr() >> projectMgr
            }
            service.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator) {
                authorizeApplicationResourceAny(auth, _, [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> {
                    return it[1].name in authed
                }
                authResourceForProject(_)>>{
                    return [name:(it[0])]
                }
            }
        when:
            def result=service.projects(auth)
        then:
            result*.name == sortedList
            result*.getProperty('project.label') == labels

        where:
            names           | authed          | sortedList      | labels
            ['z', 'y', 'x'] | ['z', 'y', 'x'] | ['x', 'y', 'z'] | ['x Label','y Label','z Label']
            ['z', 'y', 'x'] | ['z',]          | ['z']           | ['z Label']
    }

    class MockScheduledExecutionService{
        def workflows = []
        def rescheduleJobs(String uuuid, String project){

        }

        def unscheduleJobsForProject(String uuuid, String project){

        }

        def listWorkflows(def query) {
            workflows
        }
    }
}
