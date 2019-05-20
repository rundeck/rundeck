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

import com.dtolabs.rundeck.core.utils.OptsUtil
import grails.web.servlet.mvc.GrailsHttpSession
import groovy.mock.interceptor.MockFor
import rundeck.ScheduledExecutionStats
import rundeck.utils.OptionsUtil

import static org.junit.Assert.*

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.JobExec
import rundeck.ReferencedExecution
import rundeck.codecs.URIComponentCodec
import rundeck.ScheduledExecution
import rundeck.Option
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.Execution
import rundeck.services.FrameworkService

@TestFor(ScheduledExecutionController)
@Mock([ScheduledExecution,Option,Workflow,CommandExec,Execution,JobExec, ReferencedExecution, ScheduledExecutionStats, FrameworkService])
class OptionsUtilsTests {
    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = new MockFor(clazz,false)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }
    public void setUp(){

        mockCodec(URIComponentCodec)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return mockFrameworkService()}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return mockFrameworkService()}
    }
    void testEmpty(){

    }

    private void assertMap(key, map, value) {
        assertEquals "invalid ${key} ${map[key]}", value, map[key]
    }

    public void testExpandUrlOptionValueSimple() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'monkey', OptionsUtil.expandUrl(option, '${option.test1.value}', se,[test1:'monkey'])
    }
    public void testExpandUrlOptionValueUrl() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'http://some.host/path/a%20monkey', OptionsUtil.expandUrl(option, 'http://some.host/path/${option.test1.value}', se,[test1:'a monkey'])
    }
    public void testExpandUrlOptionValueParam() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'http://some.host/path/?a+monkey', OptionsUtil.expandUrl(option, 'http://some.host/path/?${option.test1.value}', se,[test1:'a monkey'])
    }
    public void testExpandUrlOptionName() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'test1', OptionsUtil.expandUrl(option, '${option.name}', se)
    }

    public void testExpandUrlJobName() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'blue', OptionsUtil.expandUrl(option, '${job.name}', se)
    }

    public void testExpandUrlJobGroup() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'some%2Fwhere', OptionsUtil.expandUrl(option, '${job.group}', se)
    }

    public void testExpandUrlJobDesc() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'a%20job', OptionsUtil.expandUrl(option, '${job.description}', se)
    }

    public void testExpandUrlJobDescParam() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals '?a+job', OptionsUtil.expandUrl(option, '?${job.description}', se)
    }

    public void testExpandUrlJobProject() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'AProject', OptionsUtil.expandUrl(option, '${job.project}', se)
    }

    public void testExpandUrlJobProp_nonexistent() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals '${job.noexist}', OptionsUtil.expandUrl(option, '${job.noexist}', se)
    }

    public void testExpandUrlJobMultipleValues() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'http://test/action?name=blue&option=test1&project=AProject',
            OptionsUtil.expandUrl(option, 'http://test/action?name=${job.name}&option=${option.name}&project=${job.project}', se)

    }

    public void testExpandUrlJobUsernameAnonymous() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'anonymous', OptionsUtil.expandUrl(option, '${job.user.name}', se)
    }

    public void testExpandUrlJobUsername() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        OptionsUtil.metaClass.static.getHttpSessionInstance = { return mockHttpSession('bob')}
        OptionsUtil.metaClass.static.httpSessionInstance = { return mockHttpSession('bob')}
        assertEquals 'bob', OptionsUtil.expandUrl(option, '${job.user.name}', se)
    }

    public void testExpandUrlJobRundeckNodename() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'server1', OptionsUtil.expandUrl(option, '${job.rundeck.nodename}', se)
    }
    public void testExpandUrlJobRundeckNodename2() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'server1', OptionsUtil.expandUrl(option, '${rundeck.nodename}', se)
    }

    public void testExpandUrlJobRundeckServerUUID() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'xyz', OptionsUtil.expandUrl(option, '${job.rundeck.serverUUID}', se)
    }

    public void testExpandUrlJobRundeckServerUUID2() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'xyz', OptionsUtil.expandUrl(option, '${rundeck.serverUUID}', se)
    }
    public void testExpandUrlJobRundeckBasedir() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller,false)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return mockFrameworkService(false)}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return mockFrameworkService(false)}
        assertEquals '/a/path', OptionsUtil.expandUrl(option, '${job.rundeck.basedir}', se,[:],false)
    }

    public void testExpandUrlJobRundeckBasedir2() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller,false)
        OptionsUtil.metaClass.static.getFrameworkServiceInstance = { return mockFrameworkService(false)}
        OptionsUtil.metaClass.static.frameworkServiceInstance = { return mockFrameworkService(false)}
        assertEquals '/a/path', OptionsUtil.expandUrl(option, '${rundeck.basedir}', se,[:],false)
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
