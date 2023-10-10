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

package rundeck.controllers


import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.servlet.mvc.GrailsHttpSession
import groovy.mock.interceptor.MockFor
import org.rundeck.app.data.providers.GormUserDataProvider
import rundeck.*
import rundeck.codecs.URIComponentCodec
import rundeck.services.FrameworkService
import rundeck.utils.OptionsUtil
import spock.lang.Specification

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class OptionsParserUtilsTests extends Specification implements ControllerUnitTest<ScheduledExecutionController>, DataTest {
    GormUserDataProvider provider = new GormUserDataProvider()

    def setupSpec() { mockDomains ScheduledExecution,Option,Workflow,CommandExec,Execution,JobExec, ReferencedExecution, ScheduledExecutionStats, User }
    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = new MockFor(clazz,false)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }
    def setup(){

        mockCodec(URIComponentCodec)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return mockFrameworkService()}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return mockFrameworkService()}
    }

    private void assertMap(key, map, value) {
        assertEquals "invalid ${key} ${map[key]}", value, map[key]
    }

    public void testExpandUrlOptionValueSimple() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'monkey', OptionsUtil.expandUrl(option, '${option.test1.value}', se, provider,[test1:'monkey'])
    }
    public void testExpandUrlOptionValueUrl() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'http://some.host/path/a%20monkey', OptionsUtil.expandUrl(option, 'http://some.host/path/${option.test1.value}', se, provider,[test1:'a monkey'])
    }
    public void testExpandUrlOptionValueParam() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'http://some.host/path/?a+monkey', OptionsUtil.expandUrl(option, 'http://some.host/path/?${option.test1.value}', se, provider,[test1:'a monkey'])
    }
    public void testExpandUrlOptionName() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'test1', OptionsUtil.expandUrl(option, '${option.name}', se, provider)
    }

    public void testExpandUrlJobName() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'blue', OptionsUtil.expandUrl(option, '${job.name}', se, provider)
    }

    public void testExpandUrlJobGroup() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'some%2Fwhere', OptionsUtil.expandUrl(option, '${job.group}', se, provider)
    }

    public void testExpandUrlJobDesc() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'a%20job', OptionsUtil.expandUrl(option, '${job.description}', se, provider)
    }

    public void testExpandUrlJobDescParam() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals '?a+job', OptionsUtil.expandUrl(option, '?${job.description}', se, provider)
    }

    public void testExpandUrlJobProject() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'AProject', OptionsUtil.expandUrl(option, '${job.project}', se, provider)
    }

    public void testExpandUrlJobProp_nonexistent() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals '${job.noexist}', OptionsUtil.expandUrl(option, '${job.noexist}', se, provider)
    }

    public void testExpandUrlJobMultipleValues() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'http://test/action?name=blue&option=test1&project=AProject',
            OptionsUtil.expandUrl(option, 'http://test/action?name=${job.name}&option=${option.name}&project=${job.project}', se, provider)

    }

    public void testExpandUrlJobUsernameAnonymous() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'anonymous', OptionsUtil.expandUrl(option, '${job.user.name}', se, provider)
    }

    public void testExpandUrlJobUsername() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        OptionsUtil.metaClass.static.getHttpSessionInstance = { return mockHttpSession('bob')}
        OptionsUtil.metaClass.static.httpSessionInstance = { return mockHttpSession('bob')}
        then:
        assertEquals 'bob', OptionsUtil.expandUrl(option, '${job.user.name}', se, provider)
    }

    public void testExpandUrlJobRundeckNodename() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'server1', OptionsUtil.expandUrl(option, '${job.rundeck.nodename}', se, provider)
    }
    public void testExpandUrlJobRundeckNodename2() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'server1', OptionsUtil.expandUrl(option, '${rundeck.nodename}', se, provider)
    }

    public void testExpandUrlJobRundeckServerUUID() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'xyz', OptionsUtil.expandUrl(option, '${job.rundeck.serverUUID}', se, provider)
    }

    public void testExpandUrlJobRundeckServerUUID2() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        then:
        assertEquals 'xyz', OptionsUtil.expandUrl(option, '${rundeck.serverUUID}', se, provider)
    }
    public void testExpandUrlJobRundeckBasedir() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller,false)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return mockFrameworkService(false)}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return mockFrameworkService(false)}
        then:
        assertEquals '/a/path', OptionsUtil.expandUrl(option, '${job.rundeck.basedir}', se, provider, [:],false)
    }

    public void testExpandUrlJobRundeckBasedir2() {
        when:
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller,false)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return mockFrameworkService(false)}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return mockFrameworkService(false)}

        then:
        assertEquals '/a/path', OptionsUtil.expandUrl(option, '${rundeck.basedir}', se, provider, [:],false)
    }

    protected List setupExpandUrlJob(def controller,boolean ishttp=true) {
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', groupPath: 'some/where',
                description: 'a job', project: 'AProject', argString: '-a b -c d')

        final Option option = new Option(name: 'test1', enforced: false)
        se.addToOptions(option)
        se.save()
        assertNotNull(option.properties)
        controller.frameworkService = mockFrameworkService(ishttp)
        [option, se]
    }

    protected def mockFrameworkService(boolean ishttp=true){
        mockWith(FrameworkService) {
            getFrameworkNodeName() {->
                'server1'
            }
            getServerUUID(1..1) {->
                'xyz'
            }
            if(!ishttp) {
                getRundeckBase(1..1) { ->
                    '/a/path'
                }
            }
            getProjectGlobals(1..1){String x->
                [:]
            }
        }
    }

    protected def mockHttpSession(String user){
        mockWith(GrailsHttpSession) {
            getUser() {->
                user
            }
        }
    }
}
