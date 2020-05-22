package rundeck

import org.grails.orm.hibernate.HibernateDatastore
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass

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

import org.junit.Test
import org.springframework.transaction.PlatformTransactionManager
import rundeck.CommandExec
import rundeck.Workflow

import static org.junit.Assert.*

/*
 * rundeck.WorkflowTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 5/14/12 11:29 AM
 * 
 */
class WorkflowTests {
    static HibernateDatastore hibernateDatastore

    PlatformTransactionManager transactionManager

    @BeforeClass
    static void setupGorm() {
        hibernateDatastore = new HibernateDatastore(CommandExec)
    }

    @AfterClass
    static void shutdownGorm() {
        hibernateDatastore.close()
    }

    @Before
    void setup() {
        transactionManager = hibernateDatastore.getTransactionManager()
    }

    @Test
    void testBasicToMap() {
        Workflow wf = new Workflow(keepgoing: true, strategy: 'node-first', commands: [new CommandExec(adhocRemoteString: 'test1'), new CommandExec(adhocLocalString: 'test2')])

        final map = wf.toMap()
        def cmds=map.remove('commands')
        assertEquals(2, cmds.size())
        assertEquals([keepgoing:true,strategy:'node-first'], map)

    }

    //test fromMap

    @Test
    void testFromMap() {
        Workflow wf = Workflow.fromMap([keepgoing: true, strategy: 'node-first', commands: [[exec: 'string'], [script: 'script']]])
        assertTrue(wf.keepgoing)
        assertEquals('node-first',wf.strategy)
        assertNotNull(wf.commands)
        assertEquals(2,wf.commands.size())
        assertTrue(wf.commands[0] instanceof CommandExec)
        assertTrue(wf.commands[1] instanceof CommandExec)
    }

    @Test
    void testFromMapWithHandlers() {
        Workflow wf = Workflow.fromMap([keepgoing: true, strategy: 'node-first', commands: [[exec: 'string',errorhandler:[exec: 'anotherstring']], [script: 'script']]])
        assertTrue(wf.keepgoing)
        assertEquals('node-first',wf.strategy)
        assertNotNull(wf.commands)
        assertEquals(2,wf.commands.size())
        assertTrue(wf.commands[0] instanceof CommandExec)
        assertTrue(wf.commands[1] instanceof CommandExec)
        assertNotNull(wf.commands[0].errorHandler)
        assertEquals("anotherstring",wf.commands[0].errorHandler.adhocRemoteString)
    }

    //test cloning
    @Test
    void testCloneConstructor(){
        Workflow wf = new Workflow(keepgoing: true, strategy: 'node-first', commands: [new CommandExec(adhocRemoteString: 'test1'), new CommandExec(adhocLocalString: 'test2')])
        Workflow wf2 = new Workflow(wf)

        assertEquals(true,wf2.keepgoing)
        assertEquals('node-first',wf2.strategy)
        assertEquals(2,wf2.commands.size())

        assertNotSame(wf.commands[0],wf2.commands[0])
        assertNotSame(wf.commands[1],wf2.commands[1])
    }

    @Test
    void testCloneConstructorHandlers(){
        final h1 = new CommandExec(adhocRemoteString: 'handle1')
        Workflow wf = new Workflow(keepgoing: true, strategy: 'node-first', commands: [new CommandExec(adhocRemoteString: 'test1',errorHandler: h1), new CommandExec(adhocLocalString: 'test2')])
        Workflow wf2 = new Workflow(wf)

        assertEquals(true,wf2.keepgoing)
        assertEquals('node-first',wf2.strategy)
        assertEquals(2,wf2.commands.size())
        assertNotNull(wf2.commands[0].errorHandler)


        assertNotSame(wf.commands[0], wf2.commands[0])
        assertNotSame(wf.commands[1], wf2.commands[1])
        assertNotSame(wf.commands[0].errorHandler, wf2.commands[0].errorHandler)
    }
}
