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
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import grails.test.mixin.TestFor
import rundeck.services.framework.RundeckProjectConfigurable
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

    static class TestConfigurableBean implements RundeckProjectConfigurable {

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
}
