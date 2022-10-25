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

import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import grails.gorm.DetachedCriteria
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.datastore.mapping.query.Query
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.springframework.context.ApplicationContext
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

import javax.security.auth.Subject
import javax.sql.DataSource
import java.sql.Connection
import java.sql.DatabaseMetaData

class ReportServiceSpec extends Specification implements ServiceUnitTest<ReportService>, DataTest {
    void setupSpec() {
        mockDomains ScheduledExecution, ReferencedExecution, CommandExec, ExecReport
    }

    def "executions history authorizations"(){
        given:
            def job1Resource = ['kind': 'job', 'group': 'agroup1', 'name': 'aname1']
            def job2Resource = ['kind': 'job', 'group': 'agroup2', 'name': 'aname2']
            def setDecision = new HashSet<Decision>()

            setDecision<< newDecisionInstance(Explanation.Code.GRANTED, true, job1Resource, 'view')
            setDecision<< newDecisionInstance(Explanation.Code.REJECTED, false, job1Resource, 'read')
            setDecision<< newDecisionInstance(Explanation.Code.REJECTED, false, job1Resource, 'view_history')
            //job 2
            setDecision<< newDecisionInstance(Explanation.Code.GRANTED, true, job2Resource, 'view')
            setDecision<< newDecisionInstance(Explanation.Code.GRANTED, true, job2Resource, 'read')
            setDecision<< newDecisionInstance(Explanation.Code.REJECTED_DENIED, false, job2Resource, 'view_history')

            def authContext = Mock(AuthContext)
            service.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                authorizeProjectResources(_, _, _, _) >> setDecision

            }
        when:
            def result = service.jobHistoryAuthorizations(authContext, 'aProject')

        then:
            result[ReportService.DENIED_VIEW_HISTORY_JOBS] == ['agroup2/aname2']

    }

    def "Test hack to avoid error ORA-01795 maximum number of expressions in a list is 1000"() {
        given:
        service.applicationContext = Mock(ApplicationContext){
            getBean(_, _) >> Mock(DataSource){
                getConnection() >> Mock(Connection){
                    getMetaData() >> Mock(DatabaseMetaData){
                        getDatabaseProductName() >> (isOracle ? "Oracle" : "otherDB")
                    }
                }
            }
        }
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()

        ScheduledExecution job2 = new ScheduledExecution(
                jobName: jobname+"example",
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job2.save()

        List execIds = []

        (0..1001).each {
            Execution e1 = new Execution(
                    project: project,
                    user: 'bob',
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    status: 'successful'

            )
            e1.save()
            execIds.push("${e1.id}")

            ReferencedExecution refexec = new ReferencedExecution(status: 'running',scheduledExecution: job, execution: e1)
            refexec.save()

            Execution e2 = new Execution(
                    project: project,
                    user: 'bob',
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    status: 'successful'

            )
            e2.save()

            ReferencedExecution refexec2 = new ReferencedExecution(status: 'running',scheduledExecution: job, execution: e2)
            refexec2.save()

            ExecReport execReport = new ExecReport(
                    jcExecId: e1.id,
                    jcJobId: job.id,
                    ctxProject: "AProject",
                    author: 'admin',
                    title: "title",
                    message: "message",
                    dateCompleted: e1.dateCompleted,
                    dateStarted: e1.dateStarted,
                    status: 'success',
                    actionType: "type"
            )
            execReport.save(flush: true, failOnError: true)
            ExecReport execReport2 = new ExecReport(
                    jcExecId: e2.id,
                    jcJobId: job2.id,
                    ctxProject: "AProject",
                    author: 'admin',
                    title: "title",
                    message: "message",
                    dateCompleted: e2.dateCompleted,
                    dateStarted: e2.dateStarted,
                    status: 'success',
                    actionType: "type"
            )
            execReport2.save(flush: true, failOnError: true)
        }
        ExecQuery query =  new ExecQuery()
        query.execIdFilter = execIds
        query.projFilter = "AProject"
        query.jobIdFilter = "${job.id}"
        query.execProjects = ["AProject"]
        when:
        List result = ExecReport.createCriteria().list {
            service.applyExecutionCriteria(query, delegate, true, job)
        }

        DetachedCriteria detachedCriteria1 =  new DetachedCriteria(ExecReport).build {
            service.applyExecutionCriteria(query, delegate, true, job)
        }

        def criteria = detachedCriteria1.getCriteria()[0].criteria[0].subquery.criteria[2].criteria[0]

        then:
        result
        result.size() == query.execIdFilter.size()
        criteriaQuery.isCase(criteria)

        where:
        isOracle| criteriaQuery
        true    | Query.In
        false   | Query.In
    }

    private Decision newDecisionInstance(
            Explanation.Code explanation,
            boolean authorized,
            Map<String, String> resource,
            String action
    ) {
        newDecisionInstance(newInstanceExplanation(explanation), authorized, resource, action)
    }

    private Decision newDecisionInstance(
            Explanation explanation,
            boolean authorized,
            Map<String, String> resource,
            String action
    ) {
        return new Decision() {
            private String representation

            public boolean isAuthorized() {
                return authorized
            }

            public Map<String, String> getResource() {
                return resource
            }

            public String getAction() {
                return action;
            }

            public Set<Attribute> getEnvironment() {
                return null
            }

            public Subject getSubject() {
                return null
            }

            public String toString() {
                if (representation == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Decision for: ");
                    builder.append("res<");
                    Iterator<Map.Entry<String, String>> riter = resource.entrySet().iterator();
                    while (riter.hasNext()) {
                        Map.Entry<String, String> s = riter.next();
                        builder.append(s.getKey()).append(':').append(s.getValue());
                        if (riter.hasNext()) {
                            builder.append(", ")
                        }
                    }

                    builder.append("> subject<")

                    builder.append("> action<")
                    builder.append("> env<")
                    builder.append(">")
                    builder.append(": authorized: ")
                    builder.append(isAuthorized())
                    builder.append(": ")
                    builder.append(explanation.toString())

                    this.representation = builder.toString()
                }
                return this.representation;
            }

            public Explanation explain() {
                return explanation
            }

            public long evaluationDuration() {
                return 0
            }
        };
    }

    Explanation newInstanceExplanation(Explanation.Code reasonId) {
        new Explanation() {

            public Explanation.Code getCode() {
                return reasonId
            }

            public void describe(PrintStream out) {
                out.println(toString())
            }

            public String toString() {
                return "\t" + "some reason => " + reasonId
            }
        }
    }
}
