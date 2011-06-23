import grails.test.GrailsUnitTestCase
/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
 
/*
 * ScheduledExecutionServiceTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 6/22/11 5:55 PM
 * 
 */
class ScheduledExServiceTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp();
    }

    /**
     * Test getByIDorUUID method.
     */
    public void testGetByIDorUUID() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        def testService = new ScheduledExecutionService()
        ScheduledExecution se = new ScheduledExecution(
            uuid: 'testUUID',
            jobName: 'blue',
            project: 'AProject',
            adhocExecution: true,
            adhocFilepath: '/this/is/a/path',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()
        long id = se.id

        ScheduledExecution.metaClass.static.findByUuid = {uuid-> uuid=='testUUID'?se:null }

        def result = testService.getByIDorUUID('testUUID')
        assertNotNull(result)
        assertEquals(se, result)

        result = testService.getByIDorUUID('testblah')
        assertNull(result)

        def result2 = testService.getByIDorUUID(id)
        assertNotNull(result2)
        assertEquals(se, result2)

        def result3 = testService.getByIDorUUID(id.toString())
        assertNotNull(result3)
        assertEquals(se, result3)
    }

    /**
     * test overlap between internal ID and UUID values, the ID should take precedence (return first)
     */
    public void testGetByIDorUUIDWithOverlap() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)

        def testService = new ScheduledExecutionService()
        ScheduledExecution se = new ScheduledExecution(
            uuid: 'testUUID',
            jobName: 'blue',
            project: 'AProject',
            adhocExecution: true,
            adhocFilepath: '/this/is/a/path',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()
        long id = se.id
        String idstr=id.toString()

        ScheduledExecution se2 = new ScheduledExecution(
            uuid: idstr,
            jobName: 'blue',
            project: 'AProject',
            adhocExecution: true,
            adhocFilepath: '/this/is/a/path',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se2.save()
        long id2 = se2.id

        ScheduledExecution.metaClass.static.findByUuid = { uuid-> uuid=='testUUID'? se : uuid==idstr?se2:null }
        assertEquals(se,ScheduledExecution.findByUuid('testUUID'))

        def result = testService.getByIDorUUID(id)
        assertNotNull(result)
        assertEquals(se, result)

        //result should be se 1 because ID has precedence
        result = testService.getByIDorUUID(idstr)
        assertNotNull(result)
        assertEquals(se, result)

        //test with se2 uuid directly, should return se1
        result = testService.getByIDorUUID(se2.uuid)
        assertNotNull(result)
        assertEquals(se, result)

        //test se2 id
        result = testService.getByIDorUUID(id2.toString())
        assertNotNull(result)
        assertEquals(se2, result)
    }
}
