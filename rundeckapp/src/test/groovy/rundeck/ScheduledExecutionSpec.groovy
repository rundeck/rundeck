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

package rundeck

import grails.test.hibernate.HibernateSpec
import org.eclipse.jetty.util.ajax.JSON
import testhelper.RundeckHibernateSpec
import static org.junit.Assert.*

/**
 * Created by greg on 10/21/15.
 */
class ScheduledExecutionSpec extends RundeckHibernateSpec
{
    List<Class> getDomainClasses() { [ScheduledExecution, Workflow, CommandExec]}
    def "has nodes selected by default"() {
        given:
            def se = new ScheduledExecution(nodesSelectedByDefault: value)

        when:
            def result = se.hasNodesSelectedByDefault()

        then:
            result == expected

        where:
            value | expected
            null  | true
            true  | true
            false | false
    }

    def "from map options have ref to job"() {
        given:
            def map = [
                    jobName: 'abc',
                    options: [
                            [name: 'test1', required: false],
                            [name: 'test2', required: false],
                    ]
            ]
        when:
            def se = ScheduledExecution.fromMap(map)

        then:
            se.options
            se.options.size() == 2
            se.options.every { it.scheduledExecution == se }
    }

    def "from map notifications urls should include httpMethod and format"() {
        given:
            def map = [
                    jobName: 'abc',
                    notification: [
                            onFailure: [urls:"url1", format: "JSON", httpMethod: "POST"]
                    ]
            ]
        when:
            def se = ScheduledExecution.fromMap(map)

        then:
            se.notifications.size() == 1
            Notification notification = se.notifications.first()
            notification.type == "url"
            notification.format == "JSON"
            notification.content == '{"urls":"url1","httpMethod":"POST"}'
    }

    void testGenerateJobScheduledName() {
        when:
        def ScheduledExecution se = new ScheduledExecution()
        def props = [jobName: "TestName", project: "TestFrameworkProject", type: "AType", command: "doCommand", argString: "-test", description: "whatever"]
        se.properties = props
        se.validate()
        def StringBuffer sb = new StringBuffer()
        se.errors.allErrors.each { sb << it.toString() }
        then:
        assertTrue "should validate: ${sb.toString()}", se.validate()
        when:
        se.save()
        then:
        assertEquals 1, ScheduledExecution.count()
        assertNotNull "id should be set: ${se.id}", se.id
        assertEquals "incorrect job name: ${se.generateJobScheduledName()}", se.id + ":TestName", se.generateJobScheduledName()
    }

    void testConstraintsRetry(){
        when:
        def ScheduledExecution se = new ScheduledExecution(
                jobName: "TestName",
                project: "TestFrameworkProject",
                argString: "-test",
                description: "whatever",
                retry:'123'
        )
        assertTrue se.validate()
        se.retry='${option.retry}'
        assertTrue se.validate()
        se.retry='123 '
        assertFalse se.validate()
        se.retry='1'
        assertTrue se.validate()
        se.retry='0'
        assertTrue se.validate()
        se.retry='-2'
        assertFalse se.validate()

        then:
        // the above asserts validate the tests
        1 == 1
    }

    void testInvalidServerNodeUUID() {
        when:
        ScheduledExecution se = createBasicScheduledExecution()
        se.serverNodeUUID = "blah"

        then:
        assertFalse(se.validate())
        assertTrue(se.errors.hasFieldErrors('serverNodeUUID'))
    }

    private ScheduledExecution createBasicScheduledExecution() {
        new ScheduledExecution(
                jobName: "test",
                groupPath: "",
                description: "",
                project: "test",
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                ),
                options: [],
        )
    }

    void testValidateBasic() {
        when:
        ScheduledExecution se = createBasicScheduledExecution()
        then:
        assertTrue(se.validate())
    }

    void testDeleteScheduleExecutionWorkflowCascadeAll() {

        when:
        WorkflowStep workflowStep = new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])

        ScheduledExecution se1 = new ScheduledExecution(
                uuid: 'test1',
                jobName: 'red color',
                project: 'Test',
                groupPath: 'some',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [workflowStep]).save(),
        )

        assert null != se1.save(flush: true)

        then:
        assertNotNull ScheduledExecution.findById(se1.id)
        assertNotNull Workflow.findById(se1.workflowId)

        when:
        se1.delete(flush: true)

        then:
        assertNull ScheduledExecution.findById(se1.id)
        assertFalse Workflow.findAll().any {Workflow w -> w.id == se1.workflowId}
    }

    void testConstraints() {
        when:
        def ScheduledExecution se = new ScheduledExecution()
        def props = [jobName: "TestName", project: "TestFrameworkProject", type: "AType", command: "doCommand", argString: "-test", description: "whatever"]
        se.properties = props
        se.validate()
        def StringBuffer sb = new StringBuffer()
        se.errors.allErrors.each { sb << it.toString() }
        assertTrue "ScheduledExecution should validate: ${sb}", se.validate()

        //change values for jobName
        se.jobName = null
        assertFalse "ScheduledExecution shouldn't validate", se.validate()

        se.jobName = ""
        assertFalse "ScheduledExecution shouldn't validate", se.validate()

        List notBlankFields = ['jobName', 'project']
        notBlankFields.each { key ->
            se = new ScheduledExecution()
            se.properties = props
            assertTrue se.validate()
            //change values for project
            se."${key}" = null
            assertFalse "ScheduledExecution shouldn't validate for null value of ${key}", se.validate()
            se."${key}" = ""
            assertFalse "ScheduledExecution shouldn't validate for blank value of ${key}", se.validate()
        }

        then:
        // above asserts validate the test
        1 == 1
    }

    void testValidateServerNodeUUID() {
        when:
        ScheduledExecution se = createBasicScheduledExecution()
        se.serverNodeUUID = UUID.randomUUID().toString()
        then:
        assertTrue(se.validate())
    }
}
