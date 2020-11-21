package rundeck

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.jobs.JobOption

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

import com.dtolabs.rundeck.core.utils.NodeSet
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import grails.web.mapping.LinkGenerator
import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.springframework.context.MessageSource
import rundeck.services.*

import static org.junit.Assert.*

//import grails.test.GrailsMock

class ExecutionService2Spec extends HibernateSpec implements ServiceUnitTest<ExecutionService> {

    List<Class> getDomainClasses() { [ScheduledExecution,Workflow,WorkflowStep,Execution,CommandExec,Option,User] }

    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = new MockFor(clazz)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }

    private UserAndRolesAuthContext createAuthContext(String user, Set<String> roles = []) {
        def mock=new MockFor(UserAndRolesAuthContext)
        mock.demand.getUsername{ user }
        mock.demand.getRoles { roles }
        mock.proxyInstance()
    }

    void testCreateExecutionRunning(){
        when:
        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assertNotNull(se.workflow.save())
        assertNotNull(se.save())
        Execution e = new Execution(project:"AProject",user:'bob',dateStarted: new Date(),dateCompleted: null,scheduledExecution: se,workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]))
        def valid=e.validate()
        e.errors.allErrors.each {println it.toString() }
        assertTrue(valid)
        assertNotNull(e.save())

//        ScheduledExecution.metaClass.static.lock={id-> return se}
//        def myCriteria = new Expando();
//        myCriteria.get = {Closure cls -> return [id:123]}
//        Execution.metaClass.static.createCriteria = {myCriteria }
//        Execution.metaClass.static.executeQuery = {q,h->[[id: 123]]}
        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){

            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }
        try{
            svc.createExecution(se,createAuthContext("user1"),null)
            fail("should fail")
        }catch(ExecutionServiceException ex){
            assertTrue(ex.message.contains('is currently being executed'))
        }

        then:
        // asserts validate above
        1 == 1
    }
    void testCreateExecutionRunningMultiple(){
        when:
        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            adhocExecution: true,
            adhocFilepath: '/this/is/a/path',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            multipleExecutions: true,
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assertNotNull(se.save())

        Execution e = new Execution(project: "AProject", user: 'bob', dateStarted: new Date(), dateCompleted: null, scheduledExecution: se, workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]))
        def valid = e.validate()
        e.errors.allErrors.each { println it.toString() }
        assertTrue(valid)
        assertNotNull(e.save())
//        ScheduledExecution.metaClass.static.lock={id-> return se}
//        ScheduledExecution.metaClass.static.withNewSession={clos-> clos.call([clear:{}])}
//        def myCriteria = new Expando();
//        myCriteria.get = {Closure cls -> return [id:123]}
//        Execution.metaClass.static.createCriteria = {myCriteria }

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }
        def execution=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user'])
        assertNotNull(execution)

        then:
        // asserts validate above
        1 == 1
    }
    void testCreateExecutionSimple(){
        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assert null!=se.save()


        ExecutionService svc = service
        svc.frameworkService = Mock(FrameworkService){
            1 * getServerUUID()
        }
        svc.scheduledExecutionService = Mock(ScheduledExecutionService){
            1 * getNodes(_,_,_)
        }
        svc.jobLifecyclePluginService = Mock(JobLifecyclePluginService){
            1 * beforeJobExecution(_,_)
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'scheduled'])
        se.refresh()
        then:
            null != (e2)
            e2.argString == '-a b -c d'
            e2.scheduledExecution == se
            null != (e2.dateStarted)
            null == (e2.dateCompleted)
            e2.user == 'user1'
            e2.executionType == 'scheduled'
            def execs = se.executions
            null != execs
            execs.contains(e2)
    }

    void testCreateExecutionSimple_userRoles() {

        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                )
        se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2 = svc.createExecution(
                se,
                createAuthContext("user1", ['a', 'b'] as Set),
                null,
                [executionType: 'scheduled']
        )

        then:
        assertNotNull(e2)
        assertEquals('user1', e2.user)
        assertEquals(['a', 'b'], e2.userRoles)

    }
    void testCreateExecutionSimpleUserExecutionType(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,['executionType':'user'])

        then:
        assertNotNull(e2)
        assertEquals('user', e2.executionType)

    }
    void testCreateExecutionScheduledUserExecutionType(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,['executionType':'user-scheduled'])

        then:
        assertNotNull(e2)
        assertEquals('user1', e2.user)
        assertEquals('user-scheduled', e2.executionType)

    }
    void testCreateExecutionRetryBasic(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
            retry:'1'
        )
        se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user','extra.option.test':'12'])

        then:
        assertNotNull(e2)
        assertEquals('1',e2.retry)
        assertEquals(0,e2.retryAttempt)
    }
    void testCreateExecutionRetryOptionValue(){

        def jobRetryValue = '${option.test}'
        def testOptionValue = '12'

        assertRetryOptionValueValid(jobRetryValue, testOptionValue,'-test 12')

        expect:
        // asserts validate above
        1 == 1
    }
    void testCreateExecutionRetryOptionValueTrimmed(){

        def jobRetryValue = '${option.test}  '//extra spaces
        def testOptionValue = '12'

        assertRetryOptionValueValid(jobRetryValue, testOptionValue,'-test 12')

        expect:
        // asserts validate above
        1 == 1
    }
    void testCreateExecutionRetryOptionValueTrimmed2(){

        def jobRetryValue = '${option.test}  '//extra spaces
        def testOptionValue = '12  ' //extra spaces

        assertRetryOptionValueValid(jobRetryValue, testOptionValue,'-test "12  "')

        expect:
        // asserts validate above
        1 == 1
    }
    void testCreateExecutionRetryOptionValueInvalid(){

        def jobRetryValue = '${option.test}'
        def testOptionValue = '12x' //invalid

        assertRetryOptionValueException(jobRetryValue, testOptionValue,'Unable to create execution: the value for \'retry\' was not a valid integer: For input string: "12x"')

        expect:
        // asserts validate above
        1 == 1
    }

    private void assertRetryOptionValueValid(String jobRetryValue, String testOptionValue, String argString) {
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                uuid: 'abc',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry: jobRetryValue
        )
        def opt1 = new Option(name: 'test', enforced: false,)
        se.addToOptions(opt1)
        if (!se.validate()) {
        }
        assertNotNull se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        Execution e2 = svc.createExecution(se,createAuthContext("user1"),null, [executionType:'user',('option.test'): testOptionValue])

        assertNotNull(e2)
        assertEquals(argString, e2.argString)
        assertEquals(se, e2.scheduledExecution)
        assertNotNull(e2.dateStarted)
        assertNull(e2.dateCompleted)
        assertEquals('12', e2.retry)
        assertEquals(0, e2.retryAttempt)
        assertEquals('user1', e2.user)
    }
    private void assertRetryOptionValueException(String jobRetryValue, String testOptionValue, String exceptionMessage) {
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                uuid: 'abc',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry: jobRetryValue
        )
        def opt1 = new Option(name: 'test', enforced: false,)
        se.addToOptions(opt1)
        if (!se.validate()) {
        }
        assertNotNull se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        try{
            Execution e2 = svc.createExecution(se,createAuthContext("user1"),null, [executionType:'user',('option.test'): testOptionValue])
            fail("expected exception")
        }catch (ExecutionServiceException e){
            assertEquals(exceptionMessage,e.message)
        }

    }

    void testCreateExecutionOverrideNodefilter(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            doNodedispatch: true,
            filter: ".*",
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user',('_replaceNodeFilters'):"true",filter:'name: monkey'])

        then:
        assertNotNull(e2)
        assertEquals('name: monkey', e2.filter)

    }
    void testCreateExecutionOverrideNodefilterOldParams(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            doNodedispatch: true,
            filter: ".*",
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user',('_replaceNodeFilters'):"true",nodeIncludeName: 'monkey'])

        then:
        assertNotNull(e2)
        assertEquals('name: monkey', e2.filter)
        assertEquals('-a b -c d', e2.argString)
    }
    void testCreateExecutionOverrideNodefilterOldParamsMulti(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            doNodedispatch: true,
            filter: ".*",
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user',('_replaceNodeFilters'):"true",nodeIncludeName: ['monkey','banana']])

        then:
        assertNotNull(e2)
        assertEquals('name: monkey,banana', e2.filter)
    }
    void testCreateExecutionJobUser(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            user:'bob',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ScheduledExecution.metaClass.static.lock={id-> return se}
        ScheduledExecution.metaClass.static.withNewSession = {clos -> clos.call([clear: {}])}
        def myCriteria = new Expando();
        myCriteria.get = {Closure cls -> return null}
        Execution.metaClass.static.createCriteria = {myCriteria }
        Execution.metaClass.static.executeQuery = {q, h -> []}


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }
        when:
        Execution e=svc.createExecution(se,createAuthContext('bob'),null,[executionType: 'user'])

        then:
        assertNotNull(e)
        assertEquals('-a b -c d',e.argString)
        assertEquals(se, e.scheduledExecution)
        assertNotNull(e.dateStarted)
        assertNull(e.dateCompleted)
        assertEquals('bob',e.user)
    }
    void testCreateExecutionAsUser(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            user:'bob',//created or scheduled job has user setting
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ScheduledExecution.metaClass.static.lock={id-> return se}
        ScheduledExecution.metaClass.static.withNewSession = {clos -> clos.call([clear: {}])}
        def myCriteria = new Expando();
        myCriteria.get = {Closure cls -> return null}
        Execution.metaClass.static.createCriteria = {myCriteria }
        Execution.metaClass.static.executeQuery = {q, h -> []}


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }
        when:
        Execution e=svc.createExecution(se,createAuthContext("user1"),null,[executionType: 'user'])
        then:
        assertNotNull(e)
        assertEquals('-a b -c d',e.argString)
        assertEquals(se, e.scheduledExecution)
        assertNotNull(e.dateStarted)
        assertNull(e.dateCompleted)
        assertEquals('user1', e.user)
    }
    void testCreateExecutionOptionsValidation(){
        ScheduledExecution se = prepare()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        assertNull(se.executions)
        when:
        Execution e=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user',argString:'-test1 asdf -test2 val2b -test4 asdf4'])
        then:
        assertNotNull(e)
        assertEquals("secure option value should not be stored",'-test1 asdf -test2 val2b -test3 val3',e.argString)
    }

    void testCreateExecutionOptionsValidation2() {

        ScheduledExecution se = prepare()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        assertNull(se.executions)
        when:
        Execution e=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user',argString:'-test2 val2b -test4 asdf4'])

        then:
        assertNotNull(e)
        assertEquals("default value should be used",'-test1 val1 -test2 val2b -test3 val3',e.argString)
    }

    void testCreateExecutionOptionsValidation3() {

        ScheduledExecution se = prepare()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        assertNull(se.executions)
        when:
        Execution e=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user',argString:'-test2 val2b -test3 monkey3'])
        then:
        assertNotNull(e)
        assertEquals('-test1 val1 -test2 val2b -test3 monkey3',e.argString)
    }

    void testCreateExecutionOptionsValidation4() {
        ScheduledExecution se = prepare()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        def ms = new StubFor(MessageSource)
////        ms.demand.getMessage { key, data, locale -> key + ":" + data.toString() + ":" + locale.toString() }
        ms.demand.asBoolean(0..99) { -> true  }
        ms.demand.asBoolean(0..99) { obj -> true  }
        ms.demand.getMessage(2) { error, data,locale -> error.toString()  }
        svc.messageSource = ms.proxyInstance()
//        svc.messageSource = mockWith(MessageSource){
//            asBoolean(0..99) { -> true }
//            asBoolean(0..99) { obj -> true }
//            getMessage(2) { error, data,locale ->
//                error.toString()
//            }
//        }
            //enforced value failure on test2
        try {
            Execution e = svc.createExecution(se,createAuthContext("user1"),null, [executionType:'user',argString: '-test2 val2D -test3 monkey4'])
            fail("shouldn't succeed")
        } catch (ExecutionServiceException e) {
            assertTrue(e.message,e.message.contains("domain.Option.validation.allowed.invalid"))
        }

        expect:
        // asserts validate above
        1 == 1
    }

    void testCreateExecutionOptionsValidation5() {

        ScheduledExecution se = prepare()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        def ms = new StubFor(MessageSource)
//        ms.demand.getMessage { key, data, locale -> key + ":" + data.toString() + ":" + locale.toString() }
        ms.demand.asBoolean(0..99) { -> true  }
        ms.demand.asBoolean(0..99) { obj -> true  }
        ms.demand.getMessage(2) { error, data,locale -> error.toString()  }
        svc.messageSource = ms.proxyInstance()
            //regex failure on test3
            try {
                Execution e = svc.createExecution(se,createAuthContext("user1"),null, [executionType:'user',argString: '-test2 val2b -test3 monkey4'])
                fail("shouldn't succeed")
            } catch (ExecutionServiceException e) {
                assertTrue(e.message,e.message.contains("domain.Option.validation.regex.invalid"))
            }
        expect:
        // asserts validate above
        1 == 1
        }

    /**
     * Create a job definition with 4 options, test1 through test4. test3-4 are required.
     * @return
     */
    private ScheduledExecution prepare() {
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assertNotNull(se.save())
        def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, realValuesUrl: "http://test.com/test")
        def opt2 = new Option(name: 'test2', defaultValue: 'val2a', enforced: true, values: ['val2c', 'val2a', 'val2b'])
        def opt3 = new Option(name: 'test3', defaultValue: 'val3', enforced: false, required: true, regex: '^.*3$')
        def opt4 = new Option(name: 'test4', defaultValue: 'val4', enforced: false, required: true, secureInput: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        assertTrue(opt3.validate())
        assertTrue(opt4.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        se.addToOptions(opt3)
        se.addToOptions(opt4)
        se.save()
        se
    }

    void testGenerateJobArgline() {
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        //test regex and optional value
        ScheduledExecution se2 = new ScheduledExecution()
        se2.addToOptions(new Option(name: 'test1', enforced: false, multivalued: true,delimiter: "+"))
        se2.addToOptions(new Option(name: 'test2', enforced: false, multivalued: true))
        se2.addToOptions(new Option(name: 'test3', enforced: false, multivalued: false))
        assertNotNull(se2.options)
        assertEquals(3, se2.options.size())

        assertEquals "-test1 \"some value\"", ExecutionService.generateJobArgline(se2, ['test1': 'some value'])
        //multivalue
        assertEquals "-test1 \"some value+another value\"", ExecutionService.generateJobArgline(se2, ['test1': ['some value','another value']])
        assertEquals "-test2 \"some value,another value\"", ExecutionService.generateJobArgline(se2, ['test2': ['some value','another value']])
        assertEquals "-test3 \"some value,another value\"", ExecutionService.generateJobArgline(se2, ['test3': ['some value','another value']])

        expect:
        // asserts validate above
        1 == 1
    }

    void testGenerateJobArglinePreservesOptionSortIndexOrder() {
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        //test regex and optional value
        ScheduledExecution se2 = new ScheduledExecution()
        se2.addToOptions(new Option(name: 'abc', enforced: false, multivalued: true,delimiter: "+"))
        se2.addToOptions(new Option(name: 'zyx', enforced: false, multivalued: true,sortIndex: 1))
        se2.addToOptions(new Option(name: 'pst', enforced: false, multivalued: false,sortIndex: 0))
        assertNotNull(se2.options)
        assertEquals(3, se2.options.size())

        assertEquals "-zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value'])
        assertEquals "-pst blah -zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah'])
        assertEquals "-pst blah -zyx value -abc elf", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah', abc:'elf'])

        expect:
        // asserts validate above
        1 == 1
    }

    void testGenerateJobArglineQuotesBlanks() {
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        //test regex and optional value
        ScheduledExecution se2 = new ScheduledExecution()
        se2.addToOptions(new Option(name: 'abc', enforced: false, multivalued: true,delimiter: "+"))
        se2.addToOptions(new Option(name: 'zyx', enforced: false, multivalued: true,sortIndex: 1))
        se2.addToOptions(new Option(name: 'pst', enforced: false, multivalued: false,sortIndex: 0))
        assertNotNull(se2.options)
        assertEquals(3, se2.options.size())

        assertEquals "-zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value'])
        assertEquals "-pst blah -zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah'])
        assertEquals "-pst blah -zyx value -abc elf", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah', abc:'elf'])

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test createContext method
     */
    void testCreateContext(){

        service.frameworkService = mockWith(FrameworkService) {
            getProjectGlobals(1..1) { project ->
                [:]
            }
            filterNodeSet(1..1) { sel, proj ->
                new NodeSetImpl()
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }

        service.storageService=mockWith(StorageService){
            storageTreeWithContext{ctx->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }

        service.executionLifecyclePluginService = mockWith(ExecutionLifecyclePluginService){

        }

        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()


        Execution se = new Execution(argString:"-test args",user:"testuser",project:"testproj", loglevel:'WARN',doNodedispatch: false)
        def val= service.createContext(se,null,null,null,null,null,(WorkflowExecutionListener)null)
        assertNotNull(val)
        assertNull(val.nodeSelector)
        assertEquals("testproj",val.frameworkProject)
        assertEquals("testuser",val.user)
        assertEquals(1,val.loglevel)
        assertNull(val.framework)
        assertNull(val.executionListener)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test createContext method
     */
    void testCreateContextDatacontext() {

        service.frameworkService = makeFrameworkMock([test: 'args']).proxyInstance()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check datacontext

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals(0,val.dataContext.job.size())
            assertNotNull(val.dataContext.option)
            assertEquals([test:"args"],val.dataContext.option)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test createContext method
     */
    void testCreateContextArgsarray() {

        def fcontrol = new MockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromArray(1..1) {String[] argString ->
            [test: 'args',test2:'monkey args']
        }

        fcontrol.demand.getProjectGlobals(1..1) {  project->
            [:]
        }
        fcontrol.demand.filterNodeSet(1..1) {sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }

        service.frameworkService = fcontrol.proxyInstance()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check datacontext, inputargs instead of argString

            Execution se = new Execution(user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, null,null,null, ['-test','args','-test2',
                    'monkey args'] as String[])
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals(0,val.dataContext.job.size())
            assertNotNull(val.dataContext.option)
            println val.dataContext.option
            assertEquals([test:"args",test2:'monkey args'],val.dataContext.option)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobData() {

        service.frameworkService = makeFrameworkMock([test: 'args']).proxyInstance()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check datacontext, include job data

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, [id:"3",name:"testjob"], null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals([id: "3", name: "testjob"],val.dataContext.job)
            assertNotNull(val.dataContext.option)
            assertEquals([test:"args"],val.dataContext.option)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobDataEmptyNodeset() {

        service.frameworkService = makeFrameworkMock([test: 'args']).proxyInstance()
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check nodeset, empty

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, [id:"3",name:"testjob"], null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNull(val.nodeSelector)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobDataNodeInclude() {

        service.frameworkService = makeFrameworkMock([test: 'args']).proxyInstance()
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check nodeset, filtered from execution obj. include name

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true, nodeIncludeName: "testnode")
            def val = service.createContext(se, null, null, null, null, [id: "3", name: "testjob"], null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertNull(val.nodeSelector.exclude.name)
            assertEquals("testnode", val.nodeSelector.include.name)

        expect:
        // asserts validate above
        1 == 1
    }

    private def makeFrameworkMock(Map argsMap) {
        def fcontrol = new MockFor(FrameworkService, true)
        fcontrol.demand.getProjectGlobals(1..1) { project ->
            [:]
        }

//        fcontrol.demand.parseOptsFromString(1..1) { argString ->
//            argsMap
//        }

        fcontrol.demand.filterNodeSet(1..1) { sel, proj ->
            new NodeSetImpl()
        }

        fcontrol
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobDataNodeExclude() {

        def fcontrol = new MockFor(FrameworkService, true)
        fcontrol.demand.getProjectGlobals(1..1) {  project->
            [:]
        }
        fcontrol.demand.filterNodeSet(1..1) {sel, proj ->
            new NodeSetImpl()
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }

        service.frameworkService = fcontrol.proxyInstance()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check nodeset, filtered from execution obj. exclude name

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true, nodeExcludeName: "testnode")
            def val = service.createContext(se, null, null, null, null, [id: "3", name: "testjob"], null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertEquals("testnode", val.nodeSelector.exclude.name)
            assertNull(val.nodeSelector.include.name)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test use of ${option.x} and ${job.y} parameter expansion in node filter tag and name filters.
     */
    void testCreateContextFilters() {
        def fcontrol = new MockFor(FrameworkService, true)
        fcontrol.demand.getProjectGlobals(1..1) {  project->
            [:]
        }
        fcontrol.demand.filterNodeSet(1..1) {sel, proj ->
            new NodeSetImpl()
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }

        service.frameworkService = fcontrol.proxyInstance()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        //basic test

            Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true,nodeIncludeName: "basic")
            def val = service.createContext(se, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                    username:'bill',project:'testproj'], null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertNull(val.nodeSelector.exclude.tags)
            assertNull(val.nodeSelector.exclude.name)
            assertNull(val.nodeSelector.include.tags)
            assertEquals("basic", val.nodeSelector.include.name)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * Test node keepgoing, threadcount filter values
     */
    void testCreateContextNodeDispatchOptions() {
        service.frameworkService = makeFrameworkMock([test: 'args',test3:'something']).proxyInstance()
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        //basic test

            Execution execution = new Execution(
                    argString: "-test args -test3 something",
                    user: "testuser",
                    project: "testproj",
                    loglevel: 'WARN',
                    doNodedispatch: true,
                    filter:"name: basic",
                    nodeThreadcount: 2,
                    nodeKeepgoing: true,
                    nodeExcludePrecedence: false,
            )
            def val = service.createContext(execution, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                    username:'bill',project:'testproj'], null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertFalse(val.nodeSelector.exclude.dominant)
            assertNotNull(val.nodeSelector.include)
            assertTrue(val.nodeSelector.include.dominant)
            assertNull(val.nodeSelector.exclude.tags)
            assertNull(val.nodeSelector.exclude.name)
            assertNull(val.nodeSelector.include.tags)
            assertEquals("basic", val.nodeSelector.include.name)
            assertEquals(2, val.threadCount)
            assertEquals(true, val.keepgoing)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * Test use of ${option.x} and ${job.y} parameter expansion in node filter tag and name filters.
     */
    void testCreateContextParameterizedFilters() {
        service.frameworkService = makeFrameworkMock([test: 'args',test3:'something']).proxyInstance()
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()


        //variable expansion in include name

            Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN',
                doNodedispatch: true,
                nodeInclude: "a,\${option.test} \${option.test3}",
                nodeIncludeName: "b,\${option.test} \${option.test3}",
                nodeIncludeTags: "c,\${option.test} \${option.test3}",
                nodeIncludeOsArch: "d,\${option.test} \${option.test3}",
                nodeIncludeOsFamily: "e,\${option.test} \${option.test3}",
                nodeIncludeOsName: "f,\${option.test} \${option.test3}",
                nodeIncludeOsVersion: "g,\${option.test} \${option.test3}",
                nodeExclude: "h,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeName: "i,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeTags: "j,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsArch: "k,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsFamily: "l,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsName: "m,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsVersion: "n,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
            )
            def val = service.createContext(se, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                    username:'bill',project:'testproj'], null, (WorkflowExecutionListener)null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertEquals("a,args something", val.nodeSelector.include.hostname)
            assertEquals("b,args something", val.nodeSelector.include.name)
            assertEquals("c,args something", val.nodeSelector.include.tags)
            assertEquals("d,args something", val.nodeSelector.include.osarch)
            assertEquals("e,args something", val.nodeSelector.include.osfamily)
            assertEquals("f,args something", val.nodeSelector.include.osname)
            assertEquals("g,args something", val.nodeSelector.include.osversion)
            assertEquals("h,3 blah something/else bill testproj", val.nodeSelector.exclude.hostname)
            assertEquals("i,3 blah something/else bill testproj", val.nodeSelector.exclude.name)
            assertEquals("j,3 blah something/else bill testproj", val.nodeSelector.exclude.tags)
            assertEquals("k,3 blah something/else bill testproj", val.nodeSelector.exclude.osarch)
            assertEquals("l,3 blah something/else bill testproj", val.nodeSelector.exclude.osfamily)
            assertEquals("m,3 blah something/else bill testproj", val.nodeSelector.exclude.osname)
            assertEquals("n,3 blah something/else bill testproj", val.nodeSelector.exclude.osversion)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * Test use of ${option.x} and ${job.y} parameter expansion in node filter tag and name filters.
     */
    void testCreateContextParameterizedAttributeFilters() {
        service.frameworkService = makeFrameworkMock([test: 'args',test3:'something']).proxyInstance()
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()



        Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN',
            doNodedispatch: true,
            filter: "monkey:a,\${option.test} !environment:b,\${option.test3},d",
        )
        def val = service.createContext(se, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                username:'bill',project:'testproj'], null, (WorkflowExecutionListener)null)
        assertNotNull(val)
        assertNotNull(val.nodeSelector)
        assertNotNull(val.nodeSelector.exclude)
        assertNotNull(val.nodeSelector.include)
        assertEquals("a,args", val.nodeSelector.include.toMap().monkey)
        assertEquals("b,something,d", val.nodeSelector.exclude.toMap().environment)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * Test use of ${option.x} parameter expansion in node filter string
     */
    void testCreateContextParameterizedWholeFilter() {

        service.frameworkService = mockWith(FrameworkService){
            getProjectGlobals(1..1) {  project->
                [:]
            }
            filterNodeSet(1..1) { sel, proj ->
                new NodeSetImpl()
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> new NodeSetImpl()
        }
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        Execution execution = new Execution(argString: "-test 'tags: args'", user: "testuser", project: "testproj", loglevel: 'WARN',
            doNodedispatch: true,
            filter: "\${option.test}",
        )
        def val = service.createContext(execution, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                username:'bill',project:'testproj'], null, (WorkflowExecutionListener)null)
        assertNotNull(val)
        assertNotNull(val.nodeSelector)
        assertNotNull(val.nodeSelector.exclude)
        assertNotNull(val.nodeSelector.include)
        assertNull( val.nodeSelector.include.toMap().name)
        assertEquals("args", val.nodeSelector.include.toMap().tags)
        assertNull(val.nodeSelector.exclude.toMap().environment)

        expect:
        // asserts validate above
        1 == 1
    }


    private ExecutionService setupCleanupService(){
        def testService = new ExecutionService()
        def mcontrol = new MockFor(MetricService, true)
        mcontrol.demand.markMeter(1..1) { classname,argString ->
        }
        testService.metricService = mcontrol.proxyInstance()

        def fcontrol = new MockFor(FrameworkService, true)
        fcontrol.demand.getFrameworkNodeName(2..2) {
            'testnode'
        }
        testService.frameworkService = fcontrol.proxyInstance()

        def rcontrol = new MockFor(ReportService, true)
        rcontrol.demand.reportExecutionResult(2..2) { map ->
            [success: true]
        }
        testService.reportService = rcontrol.proxyInstance()

        def ncontrol = new MockFor(NotificationService, true)
        ncontrol.demand.asyncTriggerJobNotification(2..2) { String trigger, schedId, Map content ->
            true
        }
        testService.notificationService = ncontrol.proxyInstance()
        return testService
    }
    void testCleanupRunningJobsNull(){
        def testService = setupCleanupService()
        def wf1=new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        assertNotNull(wf1)
        assertNotNull(wf1.commands)
        assertEquals(1,wf1.commands.size())
        Execution exec1 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf1
        )
        assertNotNull(exec1.save())
        def wf2=new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        assertNotNull(wf2)
        assertNotNull(wf2.commands)
        assertEquals(1,wf2.commands.size())
        Execution exec2 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf2,
                serverNodeUUID: UUID.randomUUID().toString()
        )
        assertNotNull(exec2.save())

        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)

        assertNull(exec2.dateCompleted)
        assertNull(exec2.status)
        assertEquals(2,Execution.findAll().size())
        assertEquals(1,Execution.findAllByDateCompletedAndServerNodeUUID(null, null).size())
        testService.cleanupRunningJobs_currentTransaction((String)null)

        Execution.withSession { session ->
            session.flush()
            exec1.refresh()
            exec2.refresh()
        }

        assertNotNull(exec1.dateCompleted)
        assertEquals("false", exec1.status)
        assertNull(exec2.dateCompleted)
        assertEquals(null, exec2.status)

        expect:
        // asserts validate above
        1 == 1
    }

    void testCleanupRunningJobsForClusterNode() {
        def testService = setupCleanupService()
        def uuid = UUID.randomUUID().toString()


        def wf1 = new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        Execution exec1 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf1
        )
        assertNotNull(exec1.save(flush: true))

        def wf2 = new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        Execution exec2 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf2,
                serverNodeUUID: uuid
        )
        assertNotNull(exec2.save(flush: true))

        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)

        assertNull(exec2.dateCompleted)
        assertNull(exec2.status)

        testService.cleanupRunningJobs_currentTransaction(uuid)

        exec1.refresh()
        exec2.refresh()
        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)
        assertNotNull(exec2.dateCompleted)
        assertEquals("false", exec2.status)

        expect:
        // asserts validate above
        1 == 1

    }

    void testCleanupRunningJobsLeavesScheduled() {
        def testService = setupCleanupService()
        def uuid = UUID.randomUUID().toString()

        def wf1 = new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        Execution exec1 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date().plus(1),
                dateCompleted: null,
                workflow: wf1,
				status: 'scheduled'
        )
        assertNotNull(exec1.save())

        assertNull(exec1.dateCompleted)
        assertEquals(ExecutionService.EXECUTION_SCHEDULED, exec1.status)

        testService.cleanupRunningJobs(uuid)

        Execution.withSession { session ->
            session.flush()
            exec1.refresh()
        }

        assertNull(exec1.dateCompleted)
        assertEquals(ExecutionService.EXECUTION_SCHEDULED, exec1.status)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * null node filter
     */
    void testOverrideJobReferenceNodeFilter_empty() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        def newctx=service.overrideJobReferenceNodeFilter(null,null, context, null, null, null, null, null, null)
        assertEquals(['x','y'],newctx.nodes.nodeNames as List)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * null node filter should not override threadcount
     */
    void testOverrideJobReferenceNodeFilter_emptyWithThreadcount() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        def newctx=service.overrideJobReferenceNodeFilter(null,null, context, null, 2, null, null, null, null)
        assertEquals(['x','y'],newctx.nodes.nodeNames as List)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * null node filter should not override keepgoing
     */
    void testOverrideJobReferenceNodeFilter_emptyWithKeepgoing() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        def newctx=service.overrideJobReferenceNodeFilter(null,null, context, null, null, true, null, null, null)
        assertEquals(['x','y'],newctx.nodes.nodeNames as List)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * set node filter
     */
    void testOverrideJobReferenceNodeFilter_filter() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['z','p']) }
        }

        def newctx=service.overrideJobReferenceNodeFilter(null,new ExecutionContextImpl() , context, 'z p', null, null, null, null, null)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * set node filter and threadcount
     */
    void testOverrideJobReferenceNodeFilter_filterAndThreadcount() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['z','p']) }
        }

        def newctx=service.overrideJobReferenceNodeFilter(null,new ExecutionContextImpl(), context, 'z p', 2, null, null, null, null)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(false,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * set node filter and threadcount and keepgoing
     */
    void testOverrideJobReferenceNodeFilter_filterAndThreadcountAndKeepgoing() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['z','p']) }
        }
        def newctx=service.overrideJobReferenceNodeFilter(null,new ExecutionContextImpl(), context, 'z p', 2, true, null, null, false)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(true,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * set node filter and threadcount and keepgoing
     */
    void testOverrideJobReferenceNodeFilter_filterAndRankAttribute() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['z','p']) }
        }
        assertEquals(null, context.nodeRankAttribute)
        assertEquals(true, context.nodeRankOrderAscending)
        def newctx=service.overrideJobReferenceNodeFilter(null,new ExecutionContextImpl(), context, 'z p', 2, true, 'rank', false, null)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(true,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)
        assertEquals('rank',newctx.nodeRankAttribute)
        assertEquals(false,newctx.nodeRankOrderAscending)

        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * set node filter and threadcount and keepgoing
     */
    void testOverrideJobReferenceNodeFilter_contextVariablesInFilter() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                assertEquals('z,p,blah',selector.includes.name)
                makeNodeSet(['z', 'p'])
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['z','p']) }
        }
        def origContext = ExecutionContextImpl.builder().
                dataContext([option:[test1:'blah']]).build()
        assertEquals(null, context.nodeRankAttribute)
        assertEquals(true, context.nodeRankOrderAscending)
        def newctx=service.overrideJobReferenceNodeFilter(null,origContext, context, 'z p ${option.test1}', 2, true, 'rank', false, false)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(true,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)
        assertEquals('rank',newctx.nodeRankAttribute)
        assertEquals(false,newctx.nodeRankOrderAscending)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * set nodeIntersect and override node filter
     * when:
     *      origContext (triggered job)
     *          nodes => a b
     *      newContext (referenced job)
     *          nodes => x y
     *      nodeFilter => a x
     *      nodeIntersect => true
     * then:
     *      overridden context for referenced job is intersection of origContext and node filter
     *          nodes => a
     */
    void testOverrideJobReferenceNodeFilter_filterAndNodeIntersect() {
        def origContext = ExecutionContextImpl.builder()
                .nodes(makeNodeSet(['a','b']))
                .nodeSelector(makeSelector("a b", 1, false))
                .threadCount(1)
                .keepgoing(false)
                .build()
        def newContext = ExecutionContextImpl.builder()
                .nodes(makeNodeSet(['x', 'y']))
                .nodeSelector(makeSelector("x y", 1, false))
                .threadCount(1)
                .keepgoing(false)
                .build()
        service.frameworkService=mockWith(FrameworkService){

        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['a']) }
        }

        def newctx=service.overrideJobReferenceNodeFilter(null,origContext, newContext, 'a x', 2, null, null, null, true)
        assertEquals(['a'] as Set,newctx.nodes.nodeNames as Set)

        expect:
        // asserts validate above
        1 == 1
    }

    /**
     * set nodeIntersect and override node filter
     * when:
     *      origContext (triggered job)
     *          nodes => a b x y
     *      newContext (referenced job)
     *          nodes => x y z
     *      nodeIntersect => true
     * then:
     *      overridden context for referenced job is intersection of origContext and newContext
     *          nodes => x y
     */
    void testOverrideJobReferenceNodeFilter_NodeIntersectWithoutFilter() {
        def origContext = ExecutionContextImpl.builder()
                .nodes(makeNodeSet(['a','b','x','y']))
                .nodeSelector(makeSelector("a b x y", 1, false))
                .threadCount(1)
                .keepgoing(false)
                .build()
        def newContext = ExecutionContextImpl.builder()
                .nodes(makeNodeSet(['x','y','z']))
                .nodeSelector(makeSelector("x y z", 1, false))
                .threadCount(10)
                .keepgoing(true)
                .build()
        service.frameworkService=mockWith(FrameworkService){
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['x','y']) }
        }

        def newctx=service.overrideJobReferenceNodeFilter(null,origContext, newContext, null, 0, null, null, null, true)
        assertEquals(['x','y'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(true,newctx.keepgoing)
        assertEquals(10,newctx.threadCount)

        expect:
        // asserts validate above
        1 == 1
    }

    protected NodesSelector makeSelector(String filter, int threadcount, boolean keepgoing) {
        def nodeset=new NodeSet()
        def filter1 = NodeSet.parseFilter(filter)
        nodeset.createInclude(filter1.include)
        nodeset.createExclude(filter1.exclude)
        nodeset.setThreadCount(threadcount)
        nodeset.setKeepgoing(keepgoing)
        return nodeset
    }
    protected INodeSet makeNodeSet(List<String> nodes) {
        def nset=new NodeSetImpl()
        nodes.each{
            nset.putNode(new NodeEntryImpl(it))
        }
        return nset;
    }

    void testcreateJobReferenceContext_simple(){
        ScheduledExecution job = prepare()

        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x', 'y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':[:],'job':['execid':'123']])
                                          .user('aUser')
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            parseOptsFromArray(1..2){String[] args->
                ['test1':'value']
            }
            getProjectGlobals(1..1) {  project->
                [:]
            }
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['x','y'])
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>> { makeNodeSet(['x','y']) }
        }
        service.storageService=mockWith(StorageService){
            storageTreeWithContext(1..1){AuthContext->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        service.fileUploadService = mockWith(FileUploadService){
            executionBeforeStart { evt, skip->
                null
            }
        }

        def newCtxt=service.createJobReferenceContext(job,null,context,['-test1','value'] as String[],null,null,null,null,null,null, false,false,true,false);

        //verify nodeset
        assertEquals(['x','y'] as Set,newCtxt.nodes.nodeNames as Set)
        assertEquals(1,newCtxt.threadCount)
        assertEquals(false,newCtxt.keepgoing)
        assertNotNull(newCtxt.dataContext['option'])

        //values from parseOptsFromArray mock
        assertEquals("expected options size incorrect",1,newCtxt.dataContext['option'].size())
        assertEquals(['test1': 'value'], newCtxt.dataContext['option'])

        //expected job data context
        assertEquals("expected job data size incorrect", 8, newCtxt.dataContext['job'].size())
        assertEquals(['id': "${job.id}",
                      'execid': '123',
                      'project': 'AProject',
                      'username':'aUser',
                      'loglevel': 'ERROR',
                      'user.name': 'aUser',
                      'name':'blue',
                      'group':'some/where'
                     ].sort().toString(), newCtxt.dataContext['job'].sort().toString())

        expect:
        // asserts validate above
        1 == 1

    }
    void testcreateJobReferenceContext_overrideNodefilter(){
        ScheduledExecution job = prepare()

        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x', 'y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':[:],'job':['execid':'123']])
                                          .user('aUser')
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            parseOptsFromArray(1..2){String[] args->
                ['test1':'value']
            }
            getProjectGlobals(1..1) {  project->
                [:]
            }
            //called by createContext
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['x','y'])
            }
            //called by overrideJobReferenceNodeFilter
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['z', 'p'])
            }
        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            2 * filterAuthorizedNodes(*_)>>>[makeNodeSet(['x','y']),makeNodeSet(['z','p'])]
        }
        service.storageService=mockWith(StorageService){
            storageTreeWithContext(1..1){AuthContext->
                null
            }
        }
        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        service.fileUploadService = mockWith(FileUploadService){
            executionBeforeStart { evt, skip->
                null
            }
        }

        assertEquals(null, context.nodeRankAttribute)
        assertEquals(true, context.nodeRankOrderAscending)
        def newCtxt=service.createJobReferenceContext(job,null,context,['-test1','value'] as String[],'z p',true,3, 'rank', false,null, false,false,true,false);

        //verify nodeset
        assertEquals(['z','p'] as Set,newCtxt.nodes.nodeNames as Set)
        assertEquals(3,newCtxt.threadCount)
        assertEquals(true,newCtxt.keepgoing)
        assertEquals('rank',newCtxt.nodeRankAttribute)
        assertEquals(false,newCtxt.nodeRankOrderAscending)

        assertNotNull(newCtxt.dataContext['option'])

        //values from parseOptsFromArray mock
        assertEquals("expected options size incorrect",1,newCtxt.dataContext['option'].size())
        assertEquals(['test1':'value'],newCtxt.dataContext['option'])

        //expected job data context
        assertEquals("expected job data size incorrect", 8, newCtxt.dataContext['job'].size())
        assertEquals(['id': job.id,
                      'execid': '123',
                      'project': 'AProject',
                      'username':'aUser',
                      'loglevel': 'ERROR',
                      'user.name': 'aUser',
                      'name':'blue',
                      'group':'some/where'
                     ].sort().toString(), newCtxt.dataContext['job'].sort().toString())
        expect:
        // asserts validate above
        1 == 1
    }
    void testcreateJobReferenceContext_argDataReferences(){
        ScheduledExecution job = prepare()

        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x', 'y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'job':['execid':'123']])
                                          .user('aUser')
                                          .build()
        def parseOptsCount=0
        service.frameworkService=mockWith(FrameworkService){
            parseOptsFromArray(1..2){String[] args->
                def argsl=args as List
                if(parseOptsCount<1){
                    assertEquals(['test1','wakeful'],argsl)
                }else{
                    assertTrue(argsl.indexOf('-test1')>=0 && argsl.indexOf('-test1')<=argsl.size()-2)
                    assertTrue(argsl.indexOf('-test2')>=0 && argsl.indexOf('-test2')<=argsl.size()-2)
                    assertTrue(argsl.indexOf('-test3')>=0 && argsl.indexOf('-test3')<=argsl.size()-2)
                    assertEquals('wakeful',argsl[argsl.indexOf('-test1')+1])
                    assertEquals('val2a',argsl[argsl.indexOf('-test2')+1])
                    assertEquals('val3',argsl[argsl.indexOf('-test3')+1])
                }
                parseOptsCount++
                ['test1':'wakeful']
            }
            getProjectGlobals(1..1) {  project->
                [:]
            }
            //called by createContext
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['x','y'])
            }

        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>>makeNodeSet(['x','y'])
        }
        service.storageService=mockWith(StorageService){
            storageTreeWithContext(1..1){AuthContext->
                null
            }
        }
        service.fileUploadService = mockWith(FileUploadService){
            executionBeforeStart { evt, skip->
                null
            }
        }

        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        def newCtxt=service.createJobReferenceContext(job,null,context,['test1','${option.monkey}'] as String[],null,null,null, null, null,null, false,false,true,false);

        //verify nodeset
        assertEquals(['x','y'] as Set,newCtxt.nodes.nodeNames as Set)
        assertEquals(1,newCtxt.threadCount)
        assertEquals(false,newCtxt.keepgoing)

        assertNotNull(newCtxt.dataContext['option'])

        //values from parseOptsFromArray mock
        assertEquals("expected options size incorrect",1,newCtxt.dataContext['option'].size())
        assertEquals(['test1':'wakeful'],newCtxt.dataContext['option'])

        //expected job data context
        assertEquals("expected job data size incorrect", 8, newCtxt.dataContext['job'].size())
        assertEquals(['id': job.id,
                      'execid': '123',
                      'project': 'AProject',
                      'username':'aUser',
                      'loglevel': 'ERROR',
                      'user.name': 'aUser',
                      'name':'blue',
                      'group':'some/where'
                     ].sort().toString(), newCtxt.dataContext['job'].sort().toString())
        expect:
        // asserts validate above
        1 == 1
    }
    /**
     * Option references for missing values should expand to blank in arglist
     */
    void testcreateJobReferenceContext_argDataReferences_blank(){
        ScheduledExecution job = prepare()

        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x', 'y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'job':['execid':'123']])
                                          .user('aUser')
                                          .build()
        def parseOptsCount=0
        service.frameworkService=mockWith(FrameworkService){
            parseOptsFromArray(1..2){String[] args->
                def argsl=args as List
                if(parseOptsCount<1){
                    assertEquals(4,argsl.size())
                }else{
                    assertTrue(argsl.indexOf('-test1')>=0 && argsl.indexOf('-test1')<=argsl.size()-2)
                    assertTrue(argsl.indexOf('-test2')>=0 && argsl.indexOf('-test2')<=argsl.size()-2)
                    assertTrue(argsl.indexOf('-test3')>=0 && argsl.indexOf('-test3')<=argsl.size()-2)
                    assertEquals('wakeful',argsl[argsl.indexOf('-test1')+1])
                    assertEquals('',argsl[argsl.indexOf('-test2')+1])
                    assertEquals('val3',argsl[argsl.indexOf('-test3')+1])
                }
                parseOptsCount++
                def opts=[:]
                def key=null
                argsl.each{v->
                    if(key){
                        opts[key]=v
                        key=null
                    }else{
                        key=v.replaceFirst('^-','')
                    }
                }
                opts
            }
            getProjectGlobals(1..1) {  project->
                [:]
            }
            //called by createContext
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['x','y'])
            }

        }
        service.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * filterAuthorizedNodes(*_)>>makeNodeSet(['x','y'])
        }

        service.fileUploadService = mockWith(FileUploadService){
            executionBeforeStart { evt, skip->
                null
            }
        }

        service.storageService=mockWith(StorageService){
            storageTreeWithContext(1..1){AuthContext->
                null
            }
        }

        service.jobStateService = mockWith(JobStateService) {
            jobServiceWithAuthContext { ctx ->
                null
            }
        }
        def newCtxt=service.createJobReferenceContext(job,null,context,
                                                      ['test1','${option.monkey}','test2','${option.balloon}'] as String[],
                                                      null,null,null, null, null,null, false,false,true,false);

        //verify nodeset
        assertEquals(['x','y'] as Set,newCtxt.nodes.nodeNames as Set)
        assertEquals(1,newCtxt.threadCount)
        assertEquals(false,newCtxt.keepgoing)

        assertNotNull(newCtxt.dataContext['option'])

        //values from parseOptsFromArray mock
        assertEquals("expected options size incorrect",3,newCtxt.dataContext['option'].size())
        assertEquals(['test1':'wakeful','test2':'','test3':'val3'],newCtxt.dataContext['option'])

        //expected job data context
        assertEquals("expected job data size incorrect", 8, newCtxt.dataContext['job'].size())
        assertEquals(['id': job.id,
                      'execid': '123',
                      'project': 'AProject',
                      'username':'aUser',
                      'loglevel': 'ERROR',
                      'user.name': 'aUser',
                      'name':'blue',
                      'group':'some/where'
                     ].sort().toString(), newCtxt.dataContext['job'].sort().toString())

        expect:
        // asserts validate above
        1 == 1

    }

    void testCreateExecutionOverrideNodeCustomfilter(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            doNodedispatch: true,
            filter: ".*",
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution e2=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user',('_replaceNodeFilters'):"true",nodeoverride: 'filter',nodefilter:'tags: linux'])

        then:
        assertNotNull(e2)
        assertEquals('tags: linux', e2.filter)
        assertEquals('-a b -c d', e2.argString)
        assertEquals(se, e2.scheduledExecution)
        assertNotNull(e2.dateStarted)
        assertNull(e2.dateCompleted)
        assertEquals('user1', e2.user)
    }

    void testCreateExecutionRetryWithDelay(){

        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry:'1',
                retryDelay: '3s'
        )
        se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService=fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        when:
        Execution ex=svc.createExecution(se,createAuthContext("user1"),null,[executionType:'user','extra.option.test':'12'])
        then:
        assertNotNull(ex)
        assertEquals('-a b -c d', ex.argString)
        assertEquals(se, ex.scheduledExecution)
        assertNotNull(ex.dateStarted)
        assertNull(ex.dateCompleted)
        assertEquals('1',ex.retry)
        assertEquals('3s',ex.retryDelay)
        assertEquals(0,ex.retryAttempt)
        assertEquals('user1', ex.user)
    }
    void testCreateExecutionRetryDelayWithOptionValue(){

        def jobRetryDelayValue = '${option.test}'
        def testOptionValue = '1s'

        assertRetryDelayOptionValueValid(jobRetryDelayValue, testOptionValue,'-test 1s')

        expect:
        // asserts validate above
        1 == 1
    }
    private void assertRetryDelayOptionValueValid(String jobRetryValue, String testOptionValue, String argString) {
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                uuid: 'abc',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry: '2',
                retryDelay: jobRetryValue
        )
        def opt1 = new Option(name: 'test', enforced: false,)
        se.addToOptions(opt1)
        if (!se.validate()) {
            System.out.println(se.errors.allErrors*.toString().join("; "))
        }
        assertNotNull se.save()


        ExecutionService svc = service
        FrameworkService fsvc = mockWith(FrameworkService){
            getServerUUID(1..1){
                null
            }
        }
        svc.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getNodes(1..1){ scheduledExecution, filter, authContext ->
                null
            }
            getOptionsFromScheduleExecutionMap(1..1){scheduledExecutionMap ->
                new TreeSet<JobOption>()
            }
        }
        svc.frameworkService = fsvc
        svc.jobLifecyclePluginService = mockWith(JobLifecyclePluginService){
            beforeJobExecution(1..1){job,event->}
        }

        Execution e2 = svc.createExecution(se,createAuthContext("user1"),null, [executionType:'user',('option.test'): testOptionValue])

        assertNotNull(e2)
        assertEquals(argString, e2.argString)
        assertEquals(se, e2.scheduledExecution)
        assertNotNull(e2.dateStarted)
        assertNull(e2.dateCompleted)
        assertEquals(testOptionValue, e2.retryDelay)
        assertEquals(0, e2.retryAttempt)
        assertEquals('user1', e2.user)
    }

    void testExportContextForExectuion(){
        def filterFixture = "foo bar"

        def ex = new Execution(
                project: "test",
                user: "test",
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                ),
                filter: filterFixture
        )

        def lg = new MockFor(LinkGenerator)
        lg.demand.link(2..2) { return '' }

        def jobcontext = ExecutionService.exportContextForExecution(ex, lg.proxyInstance())

        assertEquals(filterFixture, jobcontext.filter)

//        lg.verify()

        expect:
        // asserts validate above
        1 == 1
    }
}
