/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import grails.test.GrailsUnitTestCase
import com.dtolabs.rundeck.core.common.Framework

/*
 * ScheduledExecValidationTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: May 17, 2010 4:47:49 PM
 * $Id$
 */

public class ScheduledExecValidationTests extends GrailsUnitTestCase{

    protected void setUp() {
        super.setUp();
    }

    public void testDoValidate(){
        def sec = new ScheduledExecutionController()

        if(true){//failure on empty input
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework-> return false }
            sec.frameworkService=fwkControl.createMock()

            def params=[:]
            def results=sec._dovalidate(params)
            assertTrue(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('project'))
            assertTrue(execution.errors.hasFieldErrors('description'))
            assertTrue(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
        }
        if(true){//test basic passing input
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'a command']
            def results=sec._dovalidate(params)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertEquals 1,execution.workflow.commands.size()
            final CommandExec exec = execution.workflow.commands[0]
            assertEquals 'a command',exec.adhocRemoteString
            assertTrue exec.adhocExecution
        }
    }
    public void testDoValidateWorkflow(){
        def sec = new ScheduledExecutionController()
        if(true){//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'', 
                   workflow:[threadcount:1,keepgoing:true,"commands[0]":[adhocExecution:true,adhocRemoteString:'a remote string']]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            final Workflow wf=execution.workflow
            assertNotNull(wf)
            assertLength(1, wf.commands as Object[])
            final Iterator iterator = wf.commands.iterator()
            assert iterator.hasNext()
            final CommandExec next = iterator.next()
            assertNotNull(next)
            assertFalse(next instanceof JobExec)
            assertEquals('a remote string',next.adhocRemoteString)
        }
        if(true){//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,type:'',name:'',command:'',
                   workflow:[threadcount:1,keepgoing:true,
                       "commands[0]":[adhocExecution:true,adhocRemoteString:"do something"],
                       "commands[1]":[adhocExecution:true,adhocLocalString:"test dodah"],
                       "commands[2]":[jobName:'test1',jobGroup:'a/test'],
                   ]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            final Workflow wf=execution.workflow
            assertNotNull(wf)
            assertLength(3, wf.commands as Object[])
            final Iterator iterator = wf.commands.iterator()
            assert iterator.hasNext()
            final CommandExec next = iterator.next()
            assertNotNull(next)
            assertFalse(next instanceof JobExec)
            assertEquals('do something',next.adhocRemoteString)
            final CommandExec next2 = iterator.next()
            assertNotNull(next2)
            assertFalse(next2 instanceof JobExec)
            assertEquals('test dodah',next2.adhocLocalString)
            assertTrue('adhocExecution',next2.adhocExecution)
            final JobExec next3 = iterator.next()
            assertNotNull(next3)
            assertTrue(next3 instanceof JobExec)
            assertEquals('test1',next3.jobName)
            assertEquals('a/test',next3.jobGroup)
        }
    }
    public void testDoValidateWorkflowOptions(){
        def sec = new ScheduledExecutionController()

        if(true){//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,type:'',name:'',command:'',
                   workflow:[threadcount:1,keepgoing:true,
                       "commands[0]":[adhocExecution:true,adhocRemoteString:"do something"],
                       "commands[1]":[adhocExecution:true,adhocLocalString:"test dodah"],
                       "commands[2]":[jobName:'test1',jobGroup:'a/test'],
                   ],
                    options:["options[0]":[name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            final Workflow wf=execution.workflow
            assertNotNull(wf)
            assertLength(3, wf.commands as Object[])
            final Iterator iterator = wf.commands.iterator()
            assert iterator.hasNext()
            final CommandExec next = iterator.next()
            assertNotNull(next)
            assertFalse(next instanceof JobExec)
            assertEquals('do something',next.adhocRemoteString)
            final CommandExec next2 = iterator.next()
            assertNotNull(next2)
            assertFalse(next2 instanceof JobExec)
            assertEquals('test dodah',next2.adhocLocalString)
            assertTrue('adhocExecution',next2.adhocExecution)
            final JobExec next3 = iterator.next()
            assertNotNull(next3)
            assertTrue(next3 instanceof JobExec)
            assertEquals('test1',next3.jobName)
            assertEquals('a/test',next3.jobGroup)

            //check options
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator it2 = execution.options.iterator()
            assert it2.hasNext()
            final Option opt1 = it2.next()
            assertNotNull(opt1)
            assertEquals("wrong option name", "test3", opt1.name)
            assertEquals("wrong option name", "val3", opt1.defaultValue)
            assertNotNull("wrong option name", opt1.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", opt1.valuesUrl.toExternalForm())
            assertFalse("wrong option name", opt1.enforced)
        }
    }
    public void testDoValidateAdhoc(){
        def sec = new ScheduledExecutionController()

        if(true){//failure on missing adhoc script props
               def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:true]
            def results=sec._dovalidate(params)

            assertTrue(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)

            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('workflow'))
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def exec = execution.workflow.commands[0]
            assertTrue exec.errors.hasErrors()
            assertTrue exec.errors.hasFieldErrors('adhocExecution')

        }

        if(true){//both adhocRemote/adhocLocal should result in validation error
           def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',type:'',command:'',adhocExecution:true,adhocRemoteString:'test1',adhocLocalString:'test2']
            def results=sec._dovalidate(params)

            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)

            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('workflow'))
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def exec = execution.workflow.commands[0]
            assertTrue exec.errors.hasErrors()
            assertTrue exec.errors.hasFieldErrors('adhocRemoteString')

        }
        if(true){//test basic passing input (adhocRemoteString)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',type:'',command:'',adhocExecution:true,adhocRemoteString:'test what']
            def results=sec._dovalidate(params)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what',cexec.adhocRemoteString
            assertNull execution.adhocLocalString
            assertNull execution.adhocFilepath
            assertNull execution.argString
        }
        if(true){//test basic passing input (adhocLocalString)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocLocalString:'test what']
            def results=sec._dovalidate(params)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what',cexec.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.adhocFilepath
            assertNull execution.argString
        }
        if(true){//test basic passing input (adhocFilepath)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',type:'',command:'',adhocExecution:true,adhocFilepath:'test what']
            def results=sec._dovalidate(params)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what',cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString
        }
        if(true){//test argString input for adhocFilepath
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',type:'',command:'',
                adhocExecution:true,
                adhocFilepath:'test file',
                argString:'test args'
            ]
            def results=sec._dovalidate(params)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test file',cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertEquals 'test args',execution.argString
        }
        if(true){//test argString input for adhocRemoteString argString should be set
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',type:'',command:'',
                adhocExecution:true,
                adhocExecutionType:'remote',
                adhocRemoteString:'test remote',
                argString:'test args'
            ]
            def results=sec._dovalidate(params)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test remote',cexec.adhocRemoteString
            assertNull execution.adhocLocalString
            assertNull execution.adhocFilepath
            assertEquals 'test args',execution.argString
        }
        if(true){//test argString input for adhocLocalString
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',type:'',command:'',
                adhocExecution:true,
                adhocExecutionType:'local',
                adhocLocalString:'test local',
                argString:'test args'
            ]
            def results=sec._dovalidate(params)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test local',cexec.adhocLocalString
            assertNull execution.adhocFilepath
            assertNull execution.adhocRemoteString
            assertEquals 'test args',execution.argString
        }
    }
    public void testDoValidateNotifications(){

        def sec = new ScheduledExecutionController()
        if(true){//test job with notifications
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:"test command",
                notifications:[onsuccess:[email:'c@example.com,d@example.com']]                
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 1,execution.notifications.size()
            final def not1 = execution.notifications.iterator().next()
            assertTrue (not1 instanceof Notification)
            def Notification n = not1
            assertEquals "onsuccess",n.eventTrigger
            assertEquals "email",n.type
            assertEquals "c@example.com,d@example.com",n.content
        }
        if(true){//test job with notifications
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',
                notifications:[onsuccess:[email:'c@example.com,d@example.com'],onfailure:[email:'monkey@example.com']]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString
            
            assertNotNull execution.notifications
            assertEquals 2,execution.notifications.size()
            def nmap=[:]
            execution.notifications.each{not1->
                nmap[not1.eventTrigger]=not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess",n.eventTrigger
            assertEquals "email",n.type
            assertEquals "c@example.com,d@example.com",n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure",n2.eventTrigger
            assertEquals "email",n2.type
            assertEquals "monkey@example.com",n2.content
        }
        if(true){//test job with notifications, using form input fields for params
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',
                notifyOnsuccess:'true',notifySuccessRecipients:'c@example.com,d@example.com',
                notifyOnfailure:'true',notifyFailureRecipients:'monkey@example.com',

//                notifications:[onsuccess:[email:'c@example.com,d@example.com'],onfailure:[email:'monkey@example.com']]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString
            
            assertNotNull execution.notifications
            assertEquals 2,execution.notifications.size()
            def nmap=[:]
            execution.notifications.each{not1->
                nmap[not1.eventTrigger]=not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess",n.eventTrigger
            assertEquals "email",n.type
            assertEquals "c@example.com,d@example.com",n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure",n2.eventTrigger
            assertEquals "email",n2.type
            assertEquals "monkey@example.com",n2.content
        }

        if(true){//test job with notifications, invalid email addresses
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand',
                notifyOnsuccess:'true',notifySuccessRecipients:'c@example.',
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('notifySuccessRecipients'))
            assertFalse(execution.errors.hasFieldErrors('notifyFailureRecipients'))
        }
        if(true){//test job with notifications, invalid email addresses
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand',
                notifyOnfailure:'true',notifyFailureRecipients:'@example.com',
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('notifyFailureRecipients'))
            assertFalse(execution.errors.hasFieldErrors('notifySuccessRecipients'))
        }
        if(true){//test job with notifications, invalid email addresses using map based notifications definition
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand',
                   notifications:[onsuccess:[email:'c@example.comd@example.com'],onfailure:[email:'monkey@ example.com']]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('notifyFailureRecipients'))
            assertTrue(execution.errors.hasFieldErrors('notifySuccessRecipients'))
        }
    }
    public void testDoValidateOptions(){

        def sec = new ScheduledExecutionController()
        if(true){//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand',
                   options:["options[0]":[name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.valuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)
        }
        if(true){//test job with old-style options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',
                'command.option.test3':'val3', options: ["options[0]":[name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.valuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

            //test argString of defined command workflow item
            final Workflow wf=execution.workflow
            assertNotNull(wf)
            assertLength(1, wf.commands as Object[])
            final Iterator wfiter = wf.commands.iterator()
            assert wfiter.hasNext()
            final CommandExec wfitem = wfiter.next()
            assertNotNull(wfitem)
            assertFalse(wfitem instanceof JobExec)
            assertEquals('test command',wfitem.adhocRemoteString)
            assertEquals('-test3 ${option.test3}',wfitem.argString)
        }
        if(true){//invalid options: no name
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand',
                   options:["options[0]":[ defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            final def org.springframework.validation.Errors errors = execution.errors
            assertNotNull(errors)
            assertTrue(errors.hasErrors())
            assertTrue(errors.hasFieldErrors('options'))
            final Object rejset = errors.getFieldError('options').getRejectedValue()
            assertNotNull(rejset)
            assertLength(1,rejset as Object[])
            final Option rejopt = rejset.iterator().next()
            assertTrue(rejopt.errors.hasErrors())
            assertTrue(rejopt.errors.hasFieldErrors('name'))
        }
        if(true){//invalid options: invalid valuesUrl
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand',
                   options:["options[0]":[name:'opt3', defaultValue: 'val3', enforced: false, valuesUrl: "hzttp://test.com/test3"]]
            ]
            def results=sec._dovalidate(params)
            if(results.scheduledExecution.errors.hasErrors()){
                results.scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            final def org.springframework.validation.Errors errors = execution.errors
            assertNotNull(errors)
            assertTrue(errors.hasErrors())
            assertTrue(errors.hasFieldErrors('options'))
            final Object rejset = errors.getFieldError('options').getRejectedValue()
            assertNotNull(rejset)
            assertLength(1,rejset as Object[])
            final Option rejopt = rejset.iterator().next()
            assertTrue(rejopt.errors.hasErrors())
            assertTrue(rejopt.errors.hasFieldErrors('valuesUrl'))
        }
    }
    public void testDoUpdate(){
        def sec = new ScheduledExecutionController()
        if(true){//test update basic job details
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals 'aType2',type
                assertEquals 'aCommand2',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:true,adhocRemoteString:'test command',]
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2',execution.jobName
            assertEquals 'testProject2',execution.project
            assertEquals '',execution.description

            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNull execution.notifications
            assertNull execution.options
        }
    }
    public void testDoUpdateScheduled(){
        def sec = new ScheduledExecutionController()
        if(true){//test set scheduled with crontabString
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()
            def sesControl = mockFor(ScheduledExecutionService, true)
            sesControl.demand.scheduleJob{schedEx,oldname,oldgroup->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
                assertNotNull(schedEx)
            }
            sec.scheduledExecutionService=sesControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand',scheduled:true,crontabString:'0 21 */4 */4 */6 ? 2010-2040',useCrontabString:'true']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertTrue execution.scheduled
            assertEquals '0',execution.seconds
            assertEquals '21',execution.minute
            assertEquals '*/4',execution.hour
            assertEquals '*/4',execution.dayOfMonth
            assertEquals '*/6',execution.month
            assertEquals '?',execution.dayOfWeek
            assertEquals '2010-2040',execution.year

        }
        if(true){//test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()
            def sesControl = mockFor(ScheduledExecutionService, true)
            sesControl.demand.scheduleJob{schedEx,oldname,oldgroup->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
                assertNotNull(schedEx)
            }
            sec.scheduledExecutionService=sesControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 21 */4 */4 */6 3 2010-2040',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo, two ?)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()
            def sesControl = mockFor(ScheduledExecutionService, true)
            sesControl.demand.scheduleJob{schedEx,oldname,oldgroup->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
                assertNotNull(schedEx)
            }
            sec.scheduledExecutionService=sesControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 21 */4 ? */6 ? 2010-2040',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString (invalid year char)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 21 */4 */4 */6 ? z2010-2040',useCrontabString:'true']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')
            
        }
        if(true){//test set scheduled with invalid crontabString  (too few components)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 21 */4 */4 */6',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString  (wrong seconds value)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'70 21 */4 */4 */6 ?',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString  (wrong minutes value)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 70 */4 */4 */6 ?',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString  (wrong hour value)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 0 25 */4 */6 ?',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString  (wrong day of month value)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 0 2 32 */6 ?',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString  (wrong month value)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 0 2 3 13 ?',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if(true){//test set scheduled with invalid crontabString  (wrong day of week value)
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),scheduled:true,crontabString:'0 0 2 ? 12 8',useCrontabString:'true',jobName:'monkey1',project:'testProject',description:'',adhocExecution:false,name:'aResource',type:'aType',command:'aCommand']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
    }
    public void testDoUpdateAdhoc(){
        def sec = new ScheduledExecutionController()
        if(true){//test failure on empty adhoc params
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',
                adhocExecution:true,adhocRemoteString:'test remote',
                command:'',type:'',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals '',type
                assertEquals '',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:'true',adhocRemoteString:'']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors())
            assertTrue(execution.errors.hasFieldErrors('workflow'))
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def exec = execution.workflow.commands[0]
            assertTrue exec.errors.hasErrors()
            assertTrue exec.errors.hasFieldErrors('adhocExecution')

        }
        if(true){//test update from one adhoc type to another; remote -> local
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',
                adhocExecution:true,adhocRemoteString:'test remote',
                command:'',type:'',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals '',type
                assertEquals '',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:'true',adhocLocalString:'test local']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2',execution.jobName
            assertEquals 'testProject2',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertNull cexec.adhocFilepath
            assertEquals 'test local', cexec.adhocLocalString
            assertNull cexec.adhocRemoteString
            assertNull cexec.argString

            assertNull execution.notifications
            assertNull execution.options
        }
        if(true){//test update from one adhoc type to another; remote -> file
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',
                adhocExecution:true,adhocRemoteString:'test remote',
                command:'',type:'',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals '',type
                assertEquals '',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:'true',adhocFilepath:'test file']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2',execution.jobName
            assertEquals 'testProject2',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test file', cexec.adhocFilepath
            assertNull cexec.adhocRemoteString
            assertNull cexec.adhocLocalString
            assertNull cexec.argString

            assertNull execution.argString
            assertNull execution.notifications
            assertNull execution.options
        }
        if(true){//test update from one adhoc type to another; local -> remote
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',
                adhocExecution:true,adhocLocalString:'test local',
                command:'',type:'',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals '',type
                assertEquals '',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:'true',adhocRemoteString:'test remote']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2',execution.jobName
            assertEquals 'testProject2',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertNull cexec.adhocFilepath
            assertEquals 'test remote', cexec.adhocRemoteString
            assertNull cexec.adhocLocalString
            assertNull cexec.argString
            assertNull execution.argString
            assertNull execution.notifications
            assertNull execution.options
        }
        if(true){//test update from one adhoc type to another; local -> file
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',
                adhocExecution:true,adhocLocalString:'test local',
                command:'',type:'',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals '',type
                assertEquals '',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:'true',adhocFilepath:'test file']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2',execution.jobName
            assertEquals 'testProject2',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test file', cexec.adhocFilepath
            assertNull cexec.adhocLocalString
            assertNull cexec.adhocRemoteString
            assertNull cexec.argString
            assertNull execution.argString
            assertNull execution.notifications
            assertNull execution.options
        }
        if(true){//test update from one adhoc type to another; file -> remote
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',
                adhocExecution:true,adhocFilepath:'test file',
                command:'',type:'',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals '',type
                assertEquals '',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:'true',adhocRemoteString:'test remote']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2',execution.jobName
            assertEquals 'testProject2',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertNull cexec.adhocFilepath
            assertEquals 'test remote', cexec.adhocRemoteString
            assertNull cexec.adhocLocalString
            assertNull cexec.argString
            assertNull execution.argString
            assertNull execution.notifications
            assertNull execution.options
        }
        if(true){//test update from one adhoc type to another; file -> local
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',
                adhocExecution:true,adhocFilepath:'test file',
                command:'',type:'',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject2',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject2',project
                assertEquals '',type
                assertEquals '',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey2',project:'testProject2',description:'',adhocExecution:'true',adhocLocalString:'test local']
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2',execution.jobName
            assertEquals 'testProject2',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertNull cexec.adhocFilepath
            assertEquals 'test local', cexec.adhocLocalString
            assertNull cexec.adhocRemoteString
            assertNull cexec.argString
            assertNull execution.argString
            assertNull execution.notifications
            assertNull execution.options
        }
    }
    public void testDoUpdateNotifications(){
        def sec = new ScheduledExecutionController()
        if(true){//test update job  notifications
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',)
            def na1 = new Notification(eventTrigger:'onsuccess',type:'email',content:'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger:'onfailure',type:'email',content:'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',
                notifications:[onsuccess:[email:'spaghetti@nowhere.com'],onfailure:[email:'milk@store.com']]
            ]
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description

            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2,execution.notifications.size()
            def nmap=[:]
            execution.notifications.each{not1->
                nmap[not1.eventTrigger]=not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess",n.eventTrigger
            assertEquals "email",n.type
            assertEquals "spaghetti@nowhere.com",n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure",n2.eventTrigger
            assertEquals "email",n2.type
            assertEquals "milk@store.com",n2.content
        }

        if(true){//test update job  notifications, using form input parameters
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',)
            def na1 = new Notification(eventTrigger:'onsuccess',type:'email',content:'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger:'onfailure',type:'email',content:'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',
                notifyOnsuccess:'true',notifySuccessRecipients:'spaghetti@nowhere.com',
                notifyOnfailure:'true',notifyFailureRecipients:'milk@store.com',
            ]
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2,execution.notifications.size()
            def nmap=[:]
            execution.notifications.each{not1->
                nmap[not1.eventTrigger]=not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess",n.eventTrigger
            assertEquals "email",n.type
            assertEquals "spaghetti@nowhere.com",n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure",n2.eventTrigger
            assertEquals "email",n2.type
            assertEquals "milk@store.com",n2.content
        }
        if(true){//test update job  notifications, using form input parameters, invalid email addresses
            def se = new ScheduledExecution(jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',)
            def na1 = new Notification(eventTrigger:'onsuccess',type:'email',content:'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger:'onfailure',type:'email',content:'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
            fwkControl.demand.existsFrameworkProject{project,framework->
                assertEquals 'testProject',project
                return true
            }
            fwkControl.demand.getCommand{project,type,command,framework->
                assertEquals 'testProject',project
                assertEquals 'aType',type
                assertEquals 'aCommand',command
                return null
            }
            sec.frameworkService=fwkControl.createMock()

            def params=[id:se.id.toString(),jobName:'monkey1',project:'testProject',description:'',adhocExecution:true,adhocRemoteString:'test command',
                notifyOnsuccess:'true',notifySuccessRecipients:'spaghetti@ nowhere.com',
                notifyOnfailure:'true',notifyFailureRecipients:'milkstore.com',
            ]
            def results=sec._doupdate(params)
            def succeeded=results[0]
            def scheduledExecution=results[1]
            if(scheduledExecution && scheduledExecution.errors.hasErrors()){
                scheduledExecution.errors.allErrors.each{
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution executionErr = scheduledExecution
            assertNotNull executionErr
            assertNotNull(executionErr.errors)
            assertTrue(executionErr.errors.hasErrors())
            assertTrue(executionErr.errors.hasFieldErrors('notifyFailureRecipients'))
            assertTrue(executionErr.errors.hasFieldErrors('notifySuccessRecipients'))

            final ScheduledExecution execution = ScheduledExecution.get(scheduledExecution.id)

            assertEquals 'monkey1',execution.jobName
            assertEquals 'testProject',execution.project
            assertEquals '',execution.description
            assertEquals 'test command',execution.adhocRemoteString
            assertTrue execution.adhocExecution
            assertNotNull execution.notifications
            assertEquals 2,execution.notifications.size()
            def nmap=[:]
            execution.notifications.each{not1->
                nmap[not1.eventTrigger]=not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess",n.eventTrigger
            assertEquals "email",n.type
            assertEquals "c@example.com,d@example.com",n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure",n2.eventTrigger
            assertEquals "email",n2.type
            assertEquals "monkey@example.com",n2.content
        }
    }

    public void testDoUpdateWorkflow(){
        def sec = new ScheduledExecutionController()
        if (true) {//test update workflow 

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '')
            def workflow = new Workflow(threadcount:1,keepgoing:true)
            def wfitem = new CommandExec(adhocExecution:true,adhocRemoteString:'test command',)
            workflow.addToCommands(wfitem)
            se.workflow=workflow
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description',workflow:['commands[0]':[adhocExecution:true,adhocRemoteString:'test command2',]],'_workflow_data':true]
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            if(scheduledExecution.workflow ){
                if( scheduledExecution.workflow.errors.hasErrors()){
                    scheduledExecution.workflow.errors.allErrors.each {
                        System.err.println(it);
                    }
                }
                if(scheduledExecution.workflow.commands){
                    scheduledExecution.workflow.commands.each{cexec->
                        if(cexec.errors.hasErrors()){
                            cexec.errors.allErrors.each {
                                System.err.println(it);
                            }
                        }
                    }
                }
            }
            
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals('changed description',execution.description)
            assertNotNull(execution.workflow)
            assertLength(1,execution.workflow.commands as Object[])
            def CommandExec cexec=execution.workflow.commands[0]
            assertEquals 'test command2',cexec.adhocRemoteString
        }
    }

    public void testDoUpdateWorkflowOptions(){
        def sec = new ScheduledExecutionController()
        if (true) {//test update: update workflow by adding Options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '')
            def workflow = new Workflow(threadcount:1,keepgoing:true)
            def wfitem = new CommandExec(name: 'aResource', type: 'aType', command: 'aCommand')
            workflow.addToCommands(wfitem)
            se.workflow=workflow
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description',
                options:["options[0]":[name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals('changed description',execution.description)
            assertNotNull(execution.workflow)
            assertLength(1,execution.workflow.commands as Object[])

            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.valuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

        }
        if (true) {//test update: update workflow by removing Options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: '', type: '', command: '')
            def workflow = new Workflow(threadcount:1,keepgoing:true)
            def wfitem = new CommandExec(name: 'aResource', type: 'aType', command: 'aCommand')
            workflow.addToCommands(wfitem)
            se.workflow=workflow
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description',_nooptions:true]
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals('changed description',execution.description)
            assertNotNull(execution.workflow)
            assertLength(1,execution.workflow.commands as Object[])

            assertFalse(execution.errors.hasErrors())
            assertNull execution.options

        }
    }
    public void testDoUpdateOptions(){

        def sec = new ScheduledExecutionController()
        if (true) {//test update: don't modify existing option

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(2, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test1", next.name)
            assertEquals("wrong option name", "val1", next.defaultValue)
            assertNotNull("wrong option name", next.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test", next.valuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)
            assert iterator.hasNext()
            final Option next2 = iterator.next()
            assertNotNull(next2)
            assertEquals("wrong option name", "test2", next2.name)
            assertEquals("wrong option name", "val2", next2.defaultValue)
            assertNull("wrong option name", next2.valuesUrl)
            assertTrue("wrong option name", next2.enforced)
            assertNotNull("wrong option name", next2.values)
            assertLength(3, next2.values as Object[])
            assertArrayEquals(['a', 'b', 'c'] as String[], next2.values as String[])

        }
        if (true) {//test update: set _nooptions to delete options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand', _nooptions:true]
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNull execution.options
        }
        if (true) {//test update: set options to replace options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                options:["options[0]":[name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.valuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

        }
        if (true) {//test update: set options to replace options, use old-style command.option.name to set argString of workflow item

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '', adhocExecution:true,adhocRemoteString:'test command',)
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: '', adhocExecution:true,adhocRemoteString:'test command',
                'command.option.test3':'val3',options:["options[0]":[name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.valuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

            assertNotNull execution.workflow
            assertEquals 1,execution.workflow.commands.size()
            final CommandExec exec = execution.workflow.commands[0]
            assertEquals 'test command',exec.adhocRemoteString
            assertEquals '-test3 ${option.test3}',exec.argString

        }
        if (true) {//test update: set options to modify existing options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c'])
            def opt2 = new Option(name: 'test2', enforced: false, valuesUrl:"http://test.com/test2")
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                options:["options[0]":[name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"],
                "options[1]":[name: 'test2', defaultValue: 'val2', enforced: true, values:['a','b','c','d']]]
            ]
            def results = sec._doupdate(params)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(2, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test1", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.valuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.valuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)
            final Option next2 = iterator.next()
            assertNotNull(next2)
            assertEquals("wrong option name", "test2", next2.name)
            assertEquals("wrong option name", "val2", next2.defaultValue)
            assertNull("wrong option name", next2.valuesUrl)
            assertTrue("wrong option name", next2.enforced)

        }
    }


    public void testCopy(){
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: '', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService,true)
            seServiceControl.demand.userAuthorizedForJob {user,schedexec, framework -> return true }
            sec.scheduledExecutionService = seServiceControl.createMock()

            def params = [id: se.id.toString()]
            sec.params.putAll(params)
            sec.copy()
            assertNull sec.response.redirectedUrl
            def copied = sec.modelAndView.model.scheduledExecution
            assertNotNull(copied)
            assertEquals(se.jobName,copied.jobName)
        }
    }
}