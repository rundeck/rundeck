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
import testhelper.RundeckHibernateSpec

import javax.security.auth.Subject
import javax.sql.DataSource
import java.sql.Connection
import java.sql.DatabaseMetaData

class ReportServiceSpec extends RundeckHibernateSpec implements ServiceUnitTest<ReportService> {
    List<Class> getDomainClasses() { [ScheduledExecution, ReferencedExecution, CommandExec, ExecReport] }

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
        String project = 'Project-child'
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

        List authProjsNames = []

        (0..1001).each {
            String projectParentName = "p-parent-" + String.valueOf(it)
            Execution e1 = new Execution(
                    project: projectParentName,
                    user: 'bob',
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    status: 'successful'

            )
            e1.save()
            authProjsNames.push(projectParentName)

            ReferencedExecution refexec = new ReferencedExecution(status: 'running',scheduledExecution: job, execution: e1)
            refexec.save()

            ExecReport execReport = new ExecReport(
                    jcExecId: e1.id,
                    jcJobId: job.id,
                    ctxProject: projectParentName,
                    author: 'admin',
                    title: "title",
                    message: "message",
                    dateCompleted: e1.dateCompleted,
                    dateStarted: e1.dateStarted,
                    status: 'success',
                    actionType: "type"
            )
            execReport.save(flush: true, failOnError: true)
        }
        ExecQuery query =  new ExecQuery()
        query.authProjsFilter = authProjsNames
        query.projFilter = "AProject"
        query.jobIdFilter = "${job.id}"

        when:
        List result = ExecReport.createCriteria().list {
            service.applyExecutionCriteria(query, delegate)
        }

        DetachedCriteria detachedCriteria1 =  new DetachedCriteria(ExecReport).build {
            service.applyExecutionCriteria(query, delegate)
        }


        def criteria = detachedCriteria1.getCriteria()[0].criteria[0].criteria[1]

        then:
        result
        result.size() == query.authProjsFilter.size()
        criteriaQuery.isCase(criteria)

        where:
        isOracle| criteriaQuery
        true    | Query.Disjunction
        false   | Query.In
    }

    def "should return executions of authorized projects only"() {
        given:
        service.applicationContext = Mock(ApplicationContext){
            getBean(_, _) >> Mock(DataSource){
                getConnection() >> Mock(Connection){
                    getMetaData() >> Mock(DatabaseMetaData){
                        getDatabaseProductName() >> ("otherDB")
                    }
                }
            }
        }
        service.configurationService = Mock(ConfigurationService){
            getString("min.isolation.level", _) >> 'UNCOMMITTED'
        }

        def jobname = 'jobChild'
        String childUuid = "a1b"
        def group = 'path'
        String childProj = 'Project-child'
        ScheduledExecution childJob = new ScheduledExecution(
                jobName: jobname,
                project: childProj,
                groupPath: group,
                uuid: childUuid,
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
        childJob.save()

        List authProjsNames = [childProj]

        (0..1).each {
            String projectParentName = "p-parent-" + String.valueOf(it)
            def parentJob = new ScheduledExecution(
                    jobName: "job-parent",
                    project: projectParentName,
                    groupPath: group,
                    description: 'parent job',
                    argString: '-args b -args2 d',
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    ),
                    retry: '1'
            ).save()

            Execution e1 = new Execution(
                    project: projectParentName,
                    user: 'bob',
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    status: 'successful'
            )

            e1.save()
            if(it == 0 || (bothAuthorized && it == 1))
                authProjsNames.push(projectParentName)

            ReferencedExecution refexec = new ReferencedExecution(status: 'running',scheduledExecution: childJob, execution: e1)
            refexec.save()

            ExecReport execReport = new ExecReport(
                    jcExecId: e1.id,
                    jcJobId: parentJob.id,
                    ctxProject: parentJob.project,
                    author: 'admin',
                    title: "title",
                    message: "message",
                    dateCompleted: e1.dateCompleted,
                    dateStarted: e1.dateStarted,
                    status: 'success',
                    actionType: "type"
            )
            execReport.save(flush: true, failOnError: true)
        }

        ExecQuery query =  new ExecQuery()
        query.authProjsFilter = authProjsNames
        query.projFilter = childProj
        query.jobIdFilter = "${childJob.uuid}"
        query.max = 20
        query.offset = 0

        when:
        def result = service.getExecutionReports(query, true)

        then:
        result.reports.size() == expectedReports

        where:
        bothAuthorized | expectedReports
        true           | 2
        false          | 1
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
