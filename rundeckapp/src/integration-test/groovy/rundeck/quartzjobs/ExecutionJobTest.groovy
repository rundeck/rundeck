/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.quartzjobs

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import grails.gorm.transactions.Rollback

//import grails.test.GrailsMock
import grails.testing.mixin.integration.Integration
import groovy.mock.interceptor.MockFor
import org.junit.Assert
import org.junit.Test
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.ExecutionUtilService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulesService

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 12/9/13
 * Time: 1:10 PM
 */

@Integration
@Rollback
class ExecutionJobTest extends GroovyTestCase{
    @Test
    void testInitializeEmpty(){
        ExecutionJob job = new ExecutionJob()
        def contextMock = setupJobDataMap([:])
        try {
            job.initialize(null,contextMock)
            Assert.fail("expected exception")
        } catch (RuntimeException e) {
            Assert.assertTrue(e.message,e.message.contains("failed to lookup scheduledException object from job data map"))
        }
    }
    @Test
    void testInitializeWithoutExecutionService(){
        ScheduledExecution se = setupJob()
        ExecutionJob job = new ExecutionJob()
        def contextMock = setupJobDataMap([scheduledExecutionId:se.id])
        try {
            job.initialize(null, contextMock)
            Assert.fail("expected exception")
        } catch (RuntimeException e) {
            Assert.assertTrue(e.message,e.message.contains("ExecutionService could not be retrieved"))
        }
    }
    @Test
    void testInitializeWithoutExecutionUtilService(){
        ScheduledExecution se = setupJob()
        ExecutionJob job = new ExecutionJob()
        def mockes=new MockFor(ExecutionService)

        ExecutionService es=mockes.proxyInstance()
        def contextMock = setupJobDataMap([scheduledExecutionId:se.id,executionService:es])
        try {
            job.initialize(null, contextMock)
            Assert.fail("expected exception")
        } catch (RuntimeException e) {
            Assert.assertTrue(e.message,e.message.contains("ExecutionUtilService could not be retrieved"))
        }
    }
    /**
     * Initialize for an execution specified via job ID
     */
    @Test()
    void testInitializeJobExecution(){
        ScheduledExecution se = setupJob{se->
            se.user='test'
            se.userRoleList='a,b'
        }
        ExecutionJob job = new ExecutionJob()
        def mockes=new MockFor(ExecutionService)
        def mockeus=new MockFor(ExecutionUtilService)
        def mockfs=new MockFor(FrameworkService)
        def jobSchedulesServiceMock=new MockFor(JobSchedulesService)
        mockes.demand.selectSecureOptionInput(1..1){ ScheduledExecution scheduledExecution, Map params, Boolean exposed = false->
            [test:'input']
        }
        mockes.demand.createExecution(1..1){ ScheduledExecution se1, UserAndRolesAuthContext auth, user, input ->
            Assert.assertEquals(se.id,se1.id)
            Assert.assertEquals(se.user, auth.username)
            Assert.assertEquals(input.executionType, 'scheduled')
            'fakeExecution'
        }
        def proj = new MockFor(IRundeckProject)
        proj.demand.getProjectProperties(2..2){-> [:]}
        mockfs.demand.getFrameworkProject(1..1){project->
            proj.proxyInstance()
        }
        mockfs.demand.getRundeckFramework(1..1){->
            'fakeFramework'
        }
        def mockAuth =new MockFor(UserAndRolesAuthContext)

        mockAuth.demand.getUsername(1..1){
            'test'
        }
        mockAuth.demand.asBoolean(1..1) { true }
        def authcontext=mockAuth.proxyInstance()

        mockfs.demand.getAuthContextForUserAndRolesAndProject(1..1) { user, rolelist, project ->
            authcontext
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        FrameworkService fs = mockfs.proxyInstance()

        def contextMock = setupJobDataMap([scheduledExecutionId:se.id,frameworkService:fs,executionService:es,executionUtilService:eus,authContext:[dummy:true], jobSchedulesService: jobSchedulesServiceMock.proxyInstance()])
        def result=job.initialize(null, contextMock)

        Assert.assertEquals(se.id,result.scheduledExecutionId)
        Assert.assertEquals(se.id,result.scheduledExecution.id)
        Assert.assertEquals(es,result.executionService)
        Assert.assertEquals(eus,result.executionUtilService)
        Assert.assertEquals([test:'input'],result.secureOptsExposed)
        Assert.assertEquals("fakeFramework",result.framework)
        Assert.assertEquals("fakeExecution",result.execution)

    }

    /**
     * Job timeout determined by ScheduledExecution setting
     */
    @Test()
    void testInitializeJobExecutionWithTimeout(){
        ScheduledExecution se = setupJob{se->
            se.user='test'
            se.userRoleList='a,b'
            se.timeout='60m'
        }
        ExecutionJob job = new ExecutionJob()
        def mockes=new MockFor(ExecutionService)
        def mockeus=new MockFor(ExecutionUtilService)
        def mockfs=new MockFor(FrameworkService)
        def jobSchedulesServiceMock=new MockFor(JobSchedulesService)
        mockes.demand.selectSecureOptionInput(1..1){ ScheduledExecution scheduledExecution, Map params, Boolean exposed = false->
            [test:'input']
        }
        mockes.demand.createExecution(1..1){ ScheduledExecution se1, UserAndRolesAuthContext auth, user, input ->
            Assert.assertEquals(se.id,se1.id)
            Assert.assertEquals(se.user, auth.username)
            Assert.assertEquals(input.executionType, 'scheduled')
            'fakeExecution'
        }
        def proj = new MockFor(IRundeckProject)
        proj.demand.getProjectProperties(2..2){-> [:]}
        mockfs.demand.getFrameworkProject(1..1){project->
            proj.proxyInstance()
        }
        mockfs.demand.getRundeckFramework(1..1){->
            'fakeFramework'
        }
        def mockAuth =new MockFor(UserAndRolesAuthContext)
        mockAuth.demand.getUsername(1..1){
            'test'
        }
        mockAuth.demand.asBoolean(1..1) { true }
        def authcontext=mockAuth.proxyInstance()
        mockfs.demand.getAuthContextForUserAndRolesAndProject(1..1) { user, rolelist, project ->
            authcontext
        }

        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        FrameworkService fs = mockfs.proxyInstance()

        def contextMock = setupJobDataMap([timeout:123L,scheduledExecutionId:se.id,frameworkService:fs,executionService:es,executionUtilService:eus,authContext:[dummy:true], jobSchedulesService: jobSchedulesServiceMock.proxyInstance()])
        def result=job.initialize(null, contextMock)

        Assert.assertEquals(3600L,result.timeout)
        Assert.assertEquals("fakeExecution",result.execution)
    }

    /**
     * executeAsyncBegin fails to start, result is success=false
     */
    @Test
    void testExecuteCommandStartFailed(){
        ScheduledExecution se = setupJob()
        Execution execution = setupExecution(se, new Date(), new Date())
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution e, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            null //fail to start
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()

        def result = job.executeCommand(es, eus, execution, null, null,null,0,[:],[:])
        Assert.assertEquals(false,result.success)
    }

    /**
     * executeAsyncBegin succeeds,finish succeeds, thread succeeds
     */
    @Test
    void testExecuteCommandStartOkFinishOkThreadSuccessful(){
        ScheduledExecution se = setupJob()
        Execution execution = setupExecution(se, new Date(), new Date())
        Assert.assertNotNull(execution)
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        FrameworkService.metaClass.static.getFrameworkForUserAndRoles = { String user, List rolelist, String rundeckbase ->
            'fakeFramework'
        }
        WorkflowExecutionServiceThread stb=new TestWEServiceThread(null,null,null,null,null)
        stb.successful=true
        stb.result=wfeForSuccess(true)
        def testExecmap = [thread: stb, testExecuteAsyncBegin: true]
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution execution1, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            Assert.assertEquals(execution,execution1)
            testExecmap
        }
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockeus.demand.finishExecution(1..1){ Map datamap->
            Assert.assertTrue(datamap.testExecuteAsyncBegin)
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0

        def result=job.executeCommand(es,eus,execution,null, null, null, 0, [:], [:])
        Assert.assertEquals(true,result.success)
        Assert.assertEquals(testExecmap,result.execmap)
    }
    /**
     * executeAsyncBegin succeeds,threshold is not met
     */
    @Test
    void testExecuteCommandThresholdNotMet(){
        ScheduledExecution se = setupJob()
        Execution execution = setupExecution(se, new Date(), new Date())
        Assert.assertNotNull(execution)
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        FrameworkService.metaClass.static.getFrameworkForUserAndRoles = { String user, List rolelist, String rundeckbase ->
            'fakeFramework'
        }
        WorkflowExecutionServiceThread stb=new TestWEServiceThread(null,null,null,null,null)
        stb.successful=true
        def threshold=new testThreshold()
        def testExecmap = [thread: stb, testExecuteAsyncBegin: true, threshold:threshold]
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution execution1, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            Assert.assertEquals(execution,execution1)
            testExecmap
        }
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockeus.demand.finishExecution(1..1){ Map datamap->
            Assert.assertTrue(datamap.testExecuteAsyncBegin)
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()

        def result=job.executeCommand(es,eus,execution,null, null, null, 0, [:], [:])
        Assert.assertEquals(true,result.success)
        Assert.assertEquals(testExecmap,result.execmap)
        Assert.assertFalse(job.wasThreshold)
    }
    /**
     * executeAsyncBegin succeeds,threshold is  met, action 'fail'
     */
    @Test
    void testExecuteCommandThresholdWasMetActionHalt(){
        ScheduledExecution se = setupJob()
        Execution execution = setupExecution(se, new Date(), new Date())
        Assert.assertNotNull(execution)
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        FrameworkService.metaClass.static.getFrameworkForUserAndRoles = { String user, List rolelist, String rundeckbase ->
            'fakeFramework'
        }
        WorkflowExecutionServiceThread stb=new TestWEServiceThread(null,null,null,null,null)
        stb.successful=true
        def threshold=new testThreshold()
        threshold.wasMet=true
        threshold.action='halt'
        def testExecmap = [thread: stb, testExecuteAsyncBegin: true, threshold:threshold]
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution execution1, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            Assert.assertEquals(execution,execution1)
            stb.start()
            testExecmap
        }
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockeus.demand.finishExecution(1..1){ Map datamap->
            Assert.assertTrue(datamap.testExecuteAsyncBegin)
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()

        def result=job.executeCommand(es,eus,execution,null, null, null, 0, [:], [:])
        Assert.assertEquals(false,result.success)
        Assert.assertTrue(job.wasThreshold)
    }

    Execution setupExecution(ScheduledExecution se, Date startDate, Date finishDate) {
        Execution e
        Execution.withNewTransaction {
            e = new Execution(
                    project: "AProject",
                    user: 'bob',
                    dateStarted: startDate,
                    dateCompleted: finishDate,
                    scheduledExecution: se,
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    )
            )
            e.save()
        }
        return e
    }



    TestWFEResult wfeForSuccess(boolean success) {
        def result = new TestWFEResult()
        result.success=success
        result
    }
    /**
     * executeAsyncBegin succeeds,finish succeeds, thread fails
     */
    @Test
    void testExecuteCommandStartOkFinishOkThreadFails(){
        ScheduledExecution se = setupJob()
        Execution execution = setupExecution(se, new Date(), new Date())
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        FrameworkService.metaClass.static.getFrameworkForUserAndRoles = { String user, List rolelist, String rundeckbase ->
            'fakeFramework'
        }
        WorkflowExecutionServiceThread stb = new WorkflowExecutionServiceThread(null,null,null,null,null)
        stb.result = wfeForSuccess(false)

        def testExecmap = [thread: stb, testExecuteAsyncBegin: true]
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution execution1, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            Assert.assertEquals(execution, execution1)
            testExecmap
        }
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockeus.demand.finishExecution(1..1) { Map datamap ->
            Assert.assertTrue(datamap.testExecuteAsyncBegin)
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0

        def result = job.executeCommand(es, eus, execution, null, null, null, 0, [:], [:])
        Assert.assertEquals(false, result.success)
        Assert.assertEquals(testExecmap, result.execmap)

    }
    /**
     * executeAsyncBegin succeeds,finish fails,  retry does not succeed
     */
    @Test(expected = RuntimeException)
    void testExecuteCommandStartOkFinishRetryNoSuccess(){
        ScheduledExecution se = setupJob()
        Execution execution = setupExecution(se, new Date(), new Date())
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        FrameworkService.metaClass.static.getFrameworkForUserAndRoles = { String user, List rolelist, String rundeckbase ->
            'fakeFramework'
        }
        WorkflowExecutionServiceThread stb = new WorkflowExecutionServiceThread()
        stb.success = false
        def testExecmap = [thread: stb, testExecuteAsyncBegin: true]
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution execution1, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            Assert.assertEquals(execution, execution1)
            testExecmap
        }
        mockeus.demand.finishExecution(3..3) { Map datamap ->
            throw new Exception("expected failure")
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        job.finalizeRetryDelay=10
        job.finalizeRetryMax=3
        try {
            def result = job.executeCommand(es, eus, execution, null, null, null, 0, [:], [:])
            Assert.fail("should throw exception")
        } catch (RuntimeException e) {
            Assert.assertTrue(e.message,e.message.contains("failed"))
            throw e
        }
    }

    /**
     * executeAsyncBegin succeeds,finish fails,  retry does  succeed
     */
    @Test()
    void testExecuteCommandStartOkFinishRetryWithSuccess(){
        ScheduledExecution se = setupJob()
        Execution execution = setupExecution(se, new Date(), new Date())
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        FrameworkService.metaClass.static.getFrameworkForUserAndRoles = { String user, List rolelist, String rundeckbase ->
            'fakeFramework'
        }
        WorkflowExecutionServiceThread stb = new WorkflowExecutionServiceThread(null,null,null,null,null)
        stb.result=wfeForSuccess(false)
        def testExecmap = [thread: stb, testExecuteAsyncBegin: true]
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution execution1, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            Assert.assertEquals(execution, execution1)
            testExecmap
        }
        def count=3
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockeus.demand.finishExecution(4..4) { Map datamap ->
            if(count>0){
                count--
                throw new Exception("expected failure")
            }
        }
        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        job.finalizeRetryDelay=10
        job.finalizeRetryMax=4
        def result = job.executeCommand(es, eus, execution, null, null, null, 0, [:], [:])
        Assert.assertEquals(false, result.success)
        Assert.assertEquals(testExecmap, result.execmap)
    }

    /**
     * Closure always succeeds
     */
    def successClos = {
        true
    }
    /**
     * Closure always throws exception
     */
    def alwaysThrowClos = {
        throw new Exception("test failure")
    }
    /**
     * Always return false
     */
    def failedClos = {
        false
    }
    /**
     * Return a closure that throws an exception the first X times it is called
     */
    def throwXTimes( int max){
        int count=0
        return {
            if (max > count) {
                count++
                throw new Exception("test failure number ${count}")
            }
            true
        }
    }
    /**
     * Return a closure that throws an exception the first X times it is called
     */
    def failXTimes( int max){
        int count=0
        return {
            if (max > count) {
                count++
                return false
            }
            return true
        }
    }

    @Test
    void testWithRetrySuccessful(){
        def job=new ExecutionJob()
        def retrySuccess,exc
        (retrySuccess,exc)=job.withRetry(1,1,"test1",successClos)
        Assert.assertEquals(true,retrySuccess)
        Assert.assertNull(exc)
    }
    @Test
    void testWithRetryException(){
        def job=new ExecutionJob()
        def retrySuccess,exc
        (retrySuccess,exc)=job.withRetry(2,1,"test1",alwaysThrowClos)
        Assert.assertEquals(false,retrySuccess)
        Assert.assertNotNull(exc)
        Assert.assertEquals("test failure",exc.message)
    }
    @Test
    void testWithRetryFailure(){
        def job=new ExecutionJob()
        def retrySuccess,exc
        (retrySuccess,exc)=job.withRetry(2,1,"test1",null,failedClos)
        Assert.assertEquals(false,retrySuccess)
        Assert.assertNull(exc)
    }
    @Test
    void testWithRetryXTimesWithException(){
        def job=new ExecutionJob()
        def retrySuccess,exc
        (retrySuccess,exc)=job.withRetry(3,1,"test1",throwXTimes(3))
        Assert.assertEquals(false,retrySuccess)
        Assert.assertNotNull(exc)
        Assert.assertEquals("test failure number 3",exc.message)
    }
    @Test
    void testWithRetryXTimesWithFailure(){
        def job=new ExecutionJob()
        def retrySuccess,exc
        (retrySuccess,exc)=job.withRetry(3,1,"test1",null,failXTimes(3))
        Assert.assertEquals(false,retrySuccess)
        Assert.assertNull(exc)
    }
    @Test
    void testWithRetryXTimesWithSuccessAfterException(){
        def job=new ExecutionJob()
        def retrySuccess,exc
        (retrySuccess,exc)=job.withRetry(3,1,"test1",throwXTimes(2))
        Assert.assertEquals(true,retrySuccess)
        Assert.assertNull(exc)
    }

    @Test
    void testWithRetryXTimesWithSuccessAfterFailure(){
        def job=new ExecutionJob()
        def retrySuccess,exc
        (retrySuccess,exc)=job.withRetry(3,1,"test1",failXTimes(2))
        Assert.assertEquals(true,retrySuccess)
        Assert.assertNull(exc)
    }
    @Test
    void testSaveStateNoJob(){
        def job = new ExecutionJob()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0
        def execution = setupExecution(null, new Date(), new Date())
        def mockes = new MockFor(ExecutionService)

        def expectresult= [
                status: 'succeeded',
                cancelled: false,
                failedNodes: null,
                failedNodesMap: null,
        ]
        def execMap=[
                failedNodes: null,
        ]

        boolean saveStateCalled=false
        boolean testPass=false
        mockes.demand.saveExecutionState(1..1){ schedId, exId, Map props, Map execmap, Map retryContext->
            saveStateCalled=true
            Assert.assertNull(schedId)
            Assert.assertEquals(execution.id,exId)
            expectresult.each {k,v->
                Assert.assertEquals("result property ${k} expected: ${v} was ${props[k]}",v,props[k])
            }
            testPass=true
        }

        def es = mockes.proxyInstance()
        job.saveState(null,es,execution,true,false,false,true,null,-1,null,execMap)
        Assert.assertEquals(true,saveStateCalled)
        Assert.assertEquals(true,testPass)
    }

    @Test
    void testSaveStateThresholdCustomStatus(){
        def job = new ExecutionJob()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0
        def execution = setupExecution(null, new Date(), new Date())
        def mockes = new MockFor(ExecutionService)

        def expectresult= [
                status: 'custom',
                cancelled: false,
                failedNodes: null,
                failedNodesMap: null,
        ]
        def execMap=[
                failedNodes: null,
                threshold: [action:'halt']
        ]

        boolean saveStateCalled=false
        boolean testPass=false
        mockes.demand.saveExecutionState(1..1){ schedId, exId, Map props, Map execmap, Map retryContext->
            saveStateCalled=true
            Assert.assertNull(schedId)
            Assert.assertEquals(execution.id,exId)
            expectresult.each {k,v->
                Assert.assertEquals("result property ${k} expected: ${v} was ${props[k]}",v,props[k])
            }
            testPass=true
        }

        def es = mockes.proxyInstance()

        job.wasThreshold=true
        def initMap = [scheduledExecution: [logOutputThresholdStatus:'custom']]

        job.saveState(null,es,execution,true,false,false,true,null,-1, initMap,execMap)
        Assert.assertEquals(true,saveStateCalled)
        Assert.assertEquals(true,testPass)
    }

    @Test
    void testSaveStateWithJob(){
        def job = new ExecutionJob()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0
        def scheduledExecution = setupJob()
        def execution = setupExecution(scheduledExecution, new Date(), null)
        def mockes = new MockFor(ExecutionService)

        def expectresult= [
                status: 'succeeded',
                cancelled: false,
                failedNodes: null,
                failedNodesMap: null,
        ]
        def execMap=[
                failedNodes: null,
        ]

        boolean saveStateCalled = false
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockes.demand.saveExecutionState(1..1){ schedId, exId, Map props, Map execmap, Map retryContext->
            saveStateCalled = true
            Assert.assertEquals(scheduledExecution.id,schedId)
            Assert.assertEquals(execution.id,exId)
            expectresult.each {k,v->
                Assert.assertEquals("result property ${k} expected: ${v} was ${props[k]}",v,props[k])
            }
        }
        def x=false
        mockes.demand.updateScheduledExecStatistics(1..1){ Long schedId, long eId, long time->
            Assert.assertEquals(scheduledExecution.id,schedId)
            Assert.assertEquals(execution.id,eId)
            Assert.assertTrue(time>0)
            x=true
        }

        def es = mockes.proxyInstance()
        def result=job.saveState(null,es,execution,true,false, false, false,null, scheduledExecution.id,null,execMap)
        Assert.assertTrue(x)
        Assert.assertEquals(true, saveStateCalled)
    }

    @Test
    void testSaveStateWithJobStatsFailureRetryFail(){
        def job = new ExecutionJob()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0
        def scheduledExecution = setupJob()
        def execution = setupExecution(scheduledExecution, new Date(), null)
        def mockes = new MockFor(ExecutionService)

        def expectresult= [
                status: 'succeeded',
                cancelled: false,
                failedNodes: null,
                failedNodesMap: null,
        ]
        def execMap=[
                failedNodes: null,
        ]

        boolean saveStateCalled = false
        mockes.demand.saveExecutionState(1..1){ schedId, exId, Map props, Map execmap, Map retryContext->
            saveStateCalled = true
            Assert.assertEquals(scheduledExecution.id,schedId)
            Assert.assertEquals(execution.id,exId)
            expectresult.each {k,v->
                Assert.assertEquals("result property ${k} expected: ${v} was ${props[k]}",v,props[k])
            }
        }
        def saveStatsComplete=false
        def fail3times = throwXTimes(3)
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockes.demand.updateScheduledExecStatistics(2..2){ Long schedId, long eId, long time->
            Assert.assertEquals(scheduledExecution.id,schedId)
            Assert.assertEquals(execution.id,eId)
            Assert.assertTrue(time>0)
            fail3times()
            saveStatsComplete=true
        }

        def es = mockes.proxyInstance()
        job.statsRetryMax=2
        def result=job.saveState(null,es,execution,true,false, false,false,null, scheduledExecution.id,null,execMap)
        Assert.assertFalse(saveStatsComplete)
        Assert.assertEquals(true, saveStateCalled)
    }
    @Test
    void testSaveStateWithJobStatsFailureRetrySucceed(){
        def job = new ExecutionJob()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0
        def scheduledExecution = setupJob()
        def execution = setupExecution(scheduledExecution, new Date(), null)
        def mockes = new MockFor(ExecutionService)

        def expectresult= [
                status: 'succeeded',
                cancelled: false,
                failedNodes: null,
                failedNodesMap: null,
        ]
        def execMap=[
                failedNodes: null,
        ]

        boolean saveStateCalled = false
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockes.demand.saveExecutionState(1..1){ schedId, exId, Map props, Map execmap, Map retryContext->
            saveStateCalled = true
            Assert.assertEquals(scheduledExecution.id,schedId)
            Assert.assertEquals(execution.id,exId)
            expectresult.each {k,v->
                Assert.assertEquals("result property ${k} expected: ${v} was ${props[k]}",v,props[k])
            }
        }
        def saveStatsComplete=false
        def fail3times = throwXTimes(3)
        mockes.demand.updateScheduledExecStatistics(4..4){ Long schedId, long eId, long time->
            Assert.assertEquals(scheduledExecution.id,schedId)
            Assert.assertEquals(execution.id,eId)
            Assert.assertTrue(time>0)
            fail3times()
            saveStatsComplete=true
        }

        def es = mockes.proxyInstance()
        job.statsRetryMax=4
        def result=job.saveState(null,es,execution,true,false, false,false,null, scheduledExecution.id,null,execMap)
        Assert.assertTrue(saveStatsComplete)
        Assert.assertEquals(true, saveStateCalled)
    }

    @Test
    void testSaveStateWithFailureNoJob(){
        def job = new ExecutionJob()
        def execution = setupExecution(null, new Date(), new Date())
        def mockes = new MockFor(ExecutionService)

        def expectresult= [
                status: 'succeeded',
                cancelled: false,
                failedNodes: null,
                failedNodesMap: null,
        ]
        def execMap=[
                failedNodes: null,
        ]
        def fail3times=throwXTimes(3)

        boolean saveStateCalled = false
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockes.demand.saveExecutionState(2..2){ schedId, exId, Map props, Map execmap, Map retryContext->
            saveStateCalled = true
            Assert.assertNull(schedId)
            Assert.assertEquals(execution.id,exId)
            expectresult.each {k,v->
                Assert.assertEquals("result property ${k} expected: ${v} was ${props[k]}",v,props[k])
            }
            fail3times.call()
        }

        def es = mockes.proxyInstance()

        job.finalizeRetryMax=2
        def result = job.saveState(null, es, execution, true, false, false, true, null, -1, null, execMap)
        Assert.assertEquals(false, result)
    }


    /**
     * executeAsyncBegin succeeds,finish succeeds, thread succeeds, calls avgDurationExceeded
     */
    @Test
    void testExecuteCommandNotification(){
        ScheduledExecution se = setupJob()
        Notification notif = new Notification()
        notif.eventTrigger = "onavgduration"
        notif.type = "email"
        notif.content= '{"recipients":"test@test","subject":"test"}'
        notif.save()
        se.notifications = []
        se.notifications.add(notif)
        se.totalTime = 100
        se.execCount = 10
        se.save()
        Execution execution = setupExecution(se, new Date(), new Date())
        Assert.assertNotNull(execution)
        ExecutionJob job = new ExecutionJob()
        def mockes = new MockFor(ExecutionService)
        def mockeus = new MockFor(ExecutionUtilService)
        FrameworkService.metaClass.static.getFrameworkForUserAndRoles = { String user, List rolelist, String rundeckbase ->
            'fakeFramework'
        }
        WorkflowExecutionServiceThread stb=new TestWEServiceThread(null,null,null,null,null)
        stb.successful=true
        stb.result=wfeForSuccess(true)
        def testExecmap = [thread: stb, testExecuteAsyncBegin: true, scheduledExecution: se]
        mockes.ignore('$tt__isApplicationShutdown') { false }
        mockes.demand.executeAsyncBegin(1..1) { Framework framework, AuthContext authContext, Execution execution1, ScheduledExecution scheduledExecution = null, Map extraParams = null, Map extraParamsExposed = null ->
            Assert.assertEquals(execution,execution1)
            stb.start()
            testExecmap
        }
        mockes.demand.avgDurationExceeded(1..1){long schedId, Map content->
            true
        }
        mockeus.demand.finishExecution(1..1){ Map datamap->
            Assert.assertTrue(datamap.testExecuteAsyncBegin)
        }

        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        job.finalizeRetryMax=1
        job.finalizeRetryDelay=0

        def result=job.executeCommand(es,eus,execution,null, null, null, 0, [:], [:])
        Assert.assertEquals(true,result.success)
        Assert.assertEquals(testExecmap,result.execmap)
    }

    /**
     * Job timeout determined by ScheduledExecution option value
     */
    @Test()
    void testInitializeJobExecutionWithTimeoutFromExecution(){
        ScheduledExecution se = setupJob{se->
            se.user='test'
            se.userRoleList='a,b'
            se.timeout='${option.timeout}'
        }
        Execution e = setupExecution(se, new Date(), null)
        e.argString="-timeout=2s"
        e.save()
        ExecutionJob job = new ExecutionJob()
        def mockes=new MockFor(ExecutionService)
        def mockeus=new MockFor(ExecutionUtilService)
        def mockfs=new MockFor(FrameworkService)
        def jobSchedulesServiceMock=new MockFor(JobSchedulesService)
        mockes.demand.selectSecureOptionInput(1..1){ ScheduledExecution scheduledExecution, Map params, Boolean exposed = false->
            [test:'input']
        }
        mockes.demand.createExecution(1..1){ ScheduledExecution se1, UserAndRolesAuthContext auth, user, input ->
            Assert.assertEquals(se.id,se1.id)
            Assert.assertEquals(se.user, auth.username)
            Assert.assertEquals(input.executionType, 'scheduled')
            'fakeExecution'
        }
        def proj = new MockFor(IRundeckProject)
        proj.demand.getProjectProperties(2..2){-> [:]}
        mockfs.demand.parseOptsFromString(1..1){opts->
            ['timeout':'2s']
        }
        mockfs.demand.getRundeckFramework(1..1){->
            'fakeFramework'
        }
        def mockAuth =new MockFor(UserAndRolesAuthContext)
        mockAuth.demand.getUsername(1..1){
            'test'
        }
        mockAuth.demand.asBoolean(1..1) { true }
        def authcontext=mockAuth.proxyInstance()
        mockfs.demand.getAuthContextForUserAndRolesAndProject(1..1) { user, rolelist, project ->
            authcontext
        }

        ExecutionService es = mockes.proxyInstance()
        ExecutionUtilService eus = mockeus.proxyInstance()
        FrameworkService fs = mockfs.proxyInstance()

        def contextMock = setupJobDataMap([timeout:123L,scheduledExecutionId:se.id,executionId:e.id,frameworkService:fs,executionService:es,executionUtilService:eus,authContext:[dummy:true], jobSchedulesService: jobSchedulesServiceMock.proxyInstance()])
        def result=job.initialize(null, contextMock)

        Assert.assertEquals(2,result.timeout)
        Assert.assertEquals(e,result.execution)
    }

    private ScheduledExecution setupJob(Closure extra=null) {
        ScheduledExecution.withNewTransaction {
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
            se.dateCreated = new Date()
            se.lastUpdated = new Date()
            if (extra != null) {
                extra.call(se)
            }
            se.workflow.save()
            se.save()
        }
    }


    private def setupJobDataMap(Map mockdata) {
        def data = new Expando(mockdata)
        data.get= { String key ->
            return mockdata[key]
        }
        data.getString=data.get
        data.getBoolean={String key->
            return mockdata[key]?true:false
        }
        data.asBoolean={String key->
            return mockdata[key]?true:false
        }
        return data
    }
}
