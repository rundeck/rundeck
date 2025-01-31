package rundeck

import grails.testing.gorm.DataTest
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand

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

import rundeck.CommandExec
import rundeck.Workflow
import spock.lang.Specification

import static org.junit.Assert.*
/*
 * rundeck.WorkflowTests.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 5/14/12 11:29 AM
 *
 */

class WorkflowTests extends Specification implements DataTest {
    @Override
    Class[] getDomainClassesToMock() {
        [Workflow, CommandExec]
    }

    def testBasicToMap() {
        given:
            Workflow wf = new Workflow(
                keepgoing: true,
                strategy: 'node-first',
                commands: [new CommandExec(adhocRemoteString: 'test1'), new CommandExec(
                    adhocLocalString: 'test2'
                )]
            )
        when:

            final map = wf.toMap()
            def cmds = map.remove('commands')
        then:
            2== cmds.size()
            [keepgoing: true, strategy: 'node-first']== map

    }

    //test fromMap


    def testFromMap() {
        when:
        Workflow wf = Workflow.fromMap([keepgoing: true, strategy: 'node-first', commands: [[exec: 'string'], [script: 'script']]])
        then:
        assertTrue(wf.keepgoing)
        assertEquals('node-first',wf.strategy)
        assertNotNull(wf.commands)
        assertEquals(2,wf.commands.size())
            assertTrue(wf.commands[0] instanceof PluginStep)
            assertTrue(wf.commands[1] instanceof PluginStep)
    }


    def testFromMapWithHandlers() {
        when:
        Workflow wf = Workflow.fromMap([keepgoing: true, strategy: 'node-first', commands: [[exec: 'string',errorhandler:[exec: 'anotherstring']], [script: 'script']]])
        then:
        wf.keepgoing
        wf.strategy == 'node-first'
        wf.commands != null
        wf.commands.size() == 2
        wf.commands[0] instanceof PluginStep
        wf.commands[0].pluginType == ExecCommand.EXEC_COMMAND_TYPE
        wf.commands[0].configuration.adhocRemoteString == "string"
        wf.commands[1] instanceof PluginStep
        wf.commands[1].pluginType == ScriptCommand.SCRIPT_COMMAND_TYPE
        wf.commands[1].configuration.adhocLocalString == "script"
        wf.commands[0].errorHandler != null
        wf.commands[0].errorHandler instanceof PluginStep
        wf.commands[0].errorHandler.configuration.adhocRemoteString == "anotherstring"
    }

    //test cloning

    def testCloneConstructor(){
        when:
        Workflow wf = new Workflow(keepgoing: true, strategy: 'node-first', commands: [new CommandExec(adhocRemoteString: 'test1'), new CommandExec(adhocLocalString: 'test2')])
        Workflow wf2 = new Workflow(wf)
        then:

        assertEquals(true,wf2.keepgoing)
        assertEquals('node-first',wf2.strategy)
        assertEquals(2,wf2.commands.size())

        assertNotSame(wf.commands[0],wf2.commands[0])
        assertNotSame(wf.commands[1],wf2.commands[1])
    }


    def testCloneConstructorHandlers(){
        when:
        final h1 = new CommandExec(adhocRemoteString: 'handle1')
        Workflow wf = new Workflow(keepgoing: true, strategy: 'node-first', commands: [new CommandExec(adhocRemoteString: 'test1',errorHandler: h1), new CommandExec(adhocLocalString: 'test2')])
        Workflow wf2 = new Workflow(wf)

        then:
        assertEquals(true,wf2.keepgoing)
        assertEquals('node-first',wf2.strategy)
        assertEquals(2,wf2.commands.size())
        assertNotNull(wf2.commands[0].errorHandler)


        assertNotSame(wf.commands[0], wf2.commands[0])
        assertNotSame(wf.commands[1], wf2.commands[1])
        assertNotSame(wf.commands[0].errorHandler, wf2.commands[0].errorHandler)
    }
}
