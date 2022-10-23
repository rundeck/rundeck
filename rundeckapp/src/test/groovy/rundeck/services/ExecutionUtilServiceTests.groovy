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

import com.dtolabs.rundeck.core.execution.ServiceThreadBase
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.workflow.ControlBehavior
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLCommandExecutionItem
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import com.dtolabs.rundeck.execution.JobExecutionItem
import com.dtolabs.rundeck.execution.JobRefCommand
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Workflow
import rundeck.Execution
import spock.lang.Specification

import static org.junit.Assert.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class ExecutionUtilServiceTests extends Specification implements ServiceUnitTest<ExecutionUtilService>, DataTest{

    def setupSpec() { mockDomains Execution, CommandExec, JobExec, Workflow }


    void testItemForWFCmdItem_command(){
        when:
        def testService = service
        //exec
        CommandExec ce = new CommandExec(adhocRemoteString: 'exec command')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ExecCommandExecutionItem)
        ExecCommandExecutionItem item=(ExecCommandExecutionItem) res
        assertEquals(['exec','command'],item.command as List)

        then:
        // above asserts have validation
        1 == 1
    }

    void testItemForWFCmdItem_script() {
        when:
        def testService = service
        //adhoc local string
        CommandExec ce = new CommandExec(adhocLocalString: 'local script')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item=(ScriptFileCommandExecutionItem) res

        then:
        assertEquals('local script',item.script)
        assertNull(item.scriptAsStream)
        assertNull(item.serverScriptFilePath)
        assertNotNull(item.args)
        assertEquals(0,item.args.length)
    }

    void testItemForWFCmdItem_script_fileextension() {
        when:
        def testService = service
        //adhoc local string
        CommandExec ce = new CommandExec(adhocLocalString: 'local script',fileExtension: 'abc')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item=(ScriptFileCommandExecutionItem) res

        then:
        assertEquals('local script',item.script)
        assertNull(item.scriptAsStream)
        assertNull(item.serverScriptFilePath)
        assertNotNull(item.args)
        assertEquals(0,item.args.length)
        assertEquals('abc',item.fileExtension)
    }

    void testItemForWFCmdItem_scriptArgs() {
        when:
        def testService = service
        //adhoc local string, args
        CommandExec ce = new CommandExec(adhocLocalString: 'local script',argString: 'some args')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item=(ScriptFileCommandExecutionItem) res

        then:
        assertEquals('local script',item.script)
        assertNull(item.scriptAsStream)
        assertNull(item.serverScriptFilePath)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
    }

    void testItemForWFCmdItem_scriptfile() {
        when:
        def testService = service
        //adhoc file path
        CommandExec ce = new CommandExec(adhocFilepath: '/some/path', argString: 'some args')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item = (ScriptFileCommandExecutionItem) res

        then:
        assertEquals('/some/path', item.serverScriptFilePath)
        assertNull(item.scriptAsStream)
        assertNull(item.script)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
    }
    void testItemForWFCmdItem_scriptfile_fileextension() {
        when:
        def testService = service
        //adhoc file path
        CommandExec ce = new CommandExec(adhocFilepath: '/some/path', argString: 'some args',fileExtension: 'xyz')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item = (ScriptFileCommandExecutionItem) res

        then:
        assertEquals('/some/path', item.serverScriptFilePath)
        assertNull(item.scriptAsStream)
        assertNull(item.script)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
        assertEquals('xyz', item.fileExtension)
    }

    void testItemForWFCmdItem_scripturl() {
        when:
        def testService = service
        //http url script path
        CommandExec ce = new CommandExec(adhocFilepath: 'http://example.com/script', argString: 'some args')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res

        then:
        assertEquals('http://example.com/script', item.URLString)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
    }
    void testItemForWFCmdItem_scripturl_fileextension() {
        when:
        def testService = service
        //http url script path
        CommandExec ce = new CommandExec(adhocFilepath: 'http://example.com/script', argString: 'some args',fileExtension: 'mdd')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res

        then:
        assertEquals('http://example.com/script', item.URLString)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
        assertEquals('mdd', item.fileExtension)
    }

    void testItemForWFCmdItem_scripturl_https() {
        when:
        def testService = service
        //https url script path
        CommandExec ce = new CommandExec(adhocFilepath: 'https://example.com/script', argString: 'some args')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res

        then:
        assertEquals('https://example.com/script', item.URLString)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
    }

    void testItemForWFCmdItem_scripturl_file() {
        when:
        def testService = service
        //file url script path
        CommandExec ce = new CommandExec(adhocFilepath: 'file:/some/script')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res

        then:
        assertEquals('file:/some/script', item.URLString)
        assertNotNull(item.args)
        assertEquals(0, item.args.length)
    }

    void testItemForWFCmdItem_scripturl_file_args() {
        when:
        def testService = service
        //file url script path
        CommandExec ce = new CommandExec(adhocFilepath: 'file:/some/script', argString: 'some args')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res

        then:
        assertEquals('file:/some/script', item.URLString)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
    }

    void testItemForWFCmdItem_jobref() {
        when:
        def testService = service
        //file url script path
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobExecutionItem)
        JobExecutionItem item = (JobExecutionItem) res

        then:
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertNotNull(item.args)
        assertEquals([], item.args as List)
    }

    void testItemForWFCmdItem_jobref_args() {
        when:
        def testService = service
        //file url script path
        JobExec ce = new JobExec(
                jobName: 'abc',
                jobGroup: 'xyz',
                argString: 'abc def',
                nodeFilter: 'def',
                nodeIntersect: true
        )
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobExecutionItem)
        JobExecutionItem item = (JobExecutionItem) res

        then:
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertNotNull(item.args)
        assertEquals(['abc', 'def'], item.args as List)
        assertEquals('def', item.getNodeFilter())
        assertEquals(true, item.getNodeIntersect())

    }

    void testItemForWFCmdItem_jobref_externalProject() {
        when:
        def testService = service
        //file url script path
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz', jobProject: 'anotherProject')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobRefCommand)
        JobRefCommand item = (JobRefCommand) res

        then:
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertNotNull(item.args)
        assertEquals([], item.args as List)
    }

    void testItemForWFCmdItem_jobref_args_externalProject() {
        when:
        def testService = service
        //file url script path
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz', jobProject: 'anotherProject')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobRefCommand)
        JobRefCommand item = (JobRefCommand) res

        then:
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertEquals('anotherProject', item.getProject())
        assertNotNull(item.args)
        assertEquals([], item.args as List)
    }

    void testItemForWFCmdItem_jobref_sameproject() {
        when:
        def testService = service
        //file url script path
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz', jobProject: 'jobProject')
        def res = testService.itemForWFCmdItem(ce, null,'jobProject')
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobExecutionItem)
        JobExecutionItem item = (JobExecutionItem) res
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertNotNull(item.args)
        assertEquals([], item.args as List)
        JobRefCommand jrc = (JobRefCommand) res
        jrc.project=='jobProject'

        then:
        // above asserts have validation
        1 == 1
    }

    void testItemForWFCmdItem_jobref_otherproject() {
        when:
        def testService = service
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz',jobProject: 'refProject')
        def res = testService.itemForWFCmdItem(ce,null,'jobProject')
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobExecutionItem)
        JobExecutionItem item = (JobExecutionItem) res
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertNotNull(item.args)
        assertEquals([], item.args as List)
        JobRefCommand jrc = (JobRefCommand) res
        jrc.project=='refProject'

        then:
        // above asserts have validation
        1 == 1
    }

    void testItemForWFCmdItem_jobref_nullproject() {
        when:
        def testService = service
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz', jobProject: null)
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobExecutionItem)
        JobExecutionItem item = (JobExecutionItem) res
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertNotNull(item.args)
        assertEquals([], item.args as List)
        JobRefCommand jrc = (JobRefCommand) res
        jrc.project==null

        then:
        // above asserts have validation
        1 == 1
    }

    void testItemForWFCmdItem_jobref_setproject() {
        when:
        def testService = service
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz', jobProject: null)
        def res = testService.itemForWFCmdItem(ce, null, 'jobProject')
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertFalse(res instanceof ScriptURLCommandExecutionItem)
        assertTrue(res instanceof JobExecutionItem)
        JobExecutionItem item = (JobExecutionItem) res
        assertEquals('xyz/abc', item.getJobIdentifier())
        assertNotNull(item.args)
        assertEquals([], item.args as List)
        JobRefCommand jrc = (JobRefCommand) res
        jrc.project=='jobProject'

        then:
        // above asserts have validation
        1 == 1
    }

    void testItemForWFCmdItem_jobref_unmodified_original() {
        when:
        def testService = service
        JobExec ce = new JobExec(jobName: 'abc', jobGroup: 'xyz', jobProject: null)
        def res = testService.itemForWFCmdItem(ce, null, 'jobProject')

        then:
        assertNotNull(res)
        assertNull(ce.jobProject)
    }

    void testcreateExecutionItemForWorkflow() {
        when:
        def testService = service
        def project = 'test'
        def eh1= new JobExec([jobName: 'refhandler', jobGroup: 'grp', project: null,
                              argString: 'blah err4', keepgoingOnSuccess: false])
        Workflow w = new Workflow(
            keepgoing: true,
            commands: [
                new JobExec([jobName: 'refjob', jobGroup: 'grp', jobProject: project,
                             keepgoingOnSuccess: false ,errorHandler: eh1])
                ]
            )
        w.save()
        WorkflowExecutionItem res = testService.createExecutionItemForWorkflow(w, project)
        assertNotNull(res)
        assertNotNull(res.workflow)
        def item = res.workflow.commands[0]
        println(item)
        assertNotNull(item.failureHandler)
        println(item.failureHandler)

        then:
        assertNotNull(item.failureHandler.project)
        assertEquals(project,item.failureHandler.project)

    }
}


class MockForThreadOutputStream extends ThreadBoundOutputStream{

    /**
     * Create a ThreadBoundOutputStream with a particular OutputStream as the default write destination.
     *
     * @param stream default sink
     */
    MockForThreadOutputStream(OutputStream stream) {
        super(stream)
    }

    @Override
    OutputStream removeThreadStream() {
        return null
    }

    @Override
    void close() throws IOException {

    }
}
