package rundeck

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

/**
 * Guards how ScheduledExecutionService and JobSchedulesService narrow the criteria returned by
 * Execution.isScheduledAdHoc() and ScheduledExecution.scheduledJobs().
 *
 * Grails 8 dropped GORM named query runtime support, so those became static methods returning a
 * DetachedCriteria. Narrowing them by assigning `.where { }` back to the variable and calling
 * list() on it compiles and runs, but the extra restriction never reaches the SQL: the query goes
 * out as `select ... from execution where status=?`, with no server or project narrowing. On a
 * production-sized table every cluster member then loads every scheduled execution at startup
 * instead of its own -- a correctness problem in cluster mode, and slow enough to exceed the JDBC
 * socket timeout and abort startup.
 *
 * Chaining inline (`A().where{}.list()`) does narrow; it is the reassignment shape that loses it,
 * which is what the call sites used. They now put the restrictions in the list{} closure, which
 * narrows in both shapes and keeps the conditional structure those methods need.
 *
 * Runs against a real datasource on purpose: a DataTest unit spec cannot cover this, because its
 * in-memory GORM ignores criteria altogether and reports correct queries as broken too.
 */
@Integration
@Rollback
class ExecutionAdHocIdiomIntegrationSpec extends Specification {

    private Execution exec(String status, String uuid, String project = 'idiomtest') {
        new Execution(
            serverNodeUUID: uuid,
            dateStarted: new Date(),
            dateCompleted: null,
            failedNodeList: null,
            succeededNodeList: null,
            project: project,
            user: "user",
            status: status
        ).save(flush: true, failOnError: true)
    }

    def "restrictions in the list{} closure narrow by server and status"() {
        given:
        def mine = UUID.randomUUID().toString()
        def other = UUID.randomUUID().toString()
        String serverUUID = mine

        def wanted = exec('scheduled', mine)
        def wrongServer = exec('scheduled', other)
        def wrongStatus = exec('running', mine)

        when:
        def ids = Execution.isScheduledAdHoc().list {
            if (serverUUID) { eq 'serverNodeUUID', serverUUID }
        }*.id

        then:
        ids.contains(wanted.id)
        !ids.contains(wrongServer.id)
        !ids.contains(wrongStatus.id)
    }

    def "restrictions in the list{} closure narrow by project as well"() {
        given:
        def mine = UUID.randomUUID().toString()
        String serverUUID = mine
        String project = "proj-${UUID.randomUUID()}"

        def wanted = exec('scheduled', mine, project)
        def wrongProject = exec('scheduled', mine, 'someotherproject')

        when:
        def ids = Execution.isScheduledAdHoc().list {
            if (serverUUID) { eq 'serverNodeUUID', serverUUID }
            if (project) { eq 'project', project }
        }*.id

        then:
        ids.contains(wanted.id)
        !ids.contains(wrongProject.id)
    }

    def "an absent restriction leaves the rest of the query intact"() {
        given:
        def mine = UUID.randomUUID().toString()
        String serverUUID = null

        def scheduled = exec('scheduled', mine)
        def running = exec('running', mine)

        when: "not in cluster mode, so no server to narrow by"
        def ids = Execution.isScheduledAdHoc().list {
            if (serverUUID) { eq 'serverNodeUUID', serverUUID }
        }*.id

        then:
        ids.contains(scheduled.id)
        !ids.contains(running.id)
    }

    def "assigning .where{} back to the variable does NOT narrow -- do not use it"() {
        given:
        def other = UUID.randomUUID().toString()
        def mine = UUID.randomUUID().toString()

        exec('scheduled', mine)
        def wrongServer = exec('scheduled', other)

        when: "the shape the call sites used before the fix, with the value arriving as a parameter"
        def ids = narrowByReassigning(mine)

        then: "the other server's row still comes back: the restriction never reached the SQL"
        ids.contains(wrongServer.id)
    }

    /**
     * The broken shape, kept executable so the guard above fails if it ever starts narrowing
     * (at which point the call sites could go back to the more readable composition).
     * Chaining inline -- A().where{}.list() -- does narrow; it is assigning the result back and
     * calling list() on the variable that loses the restriction.
     */
    private List narrowByReassigning(String serverUUID) {
        def results = Execution.isScheduledAdHoc()
        if (serverUUID) {
            results = results.where { serverNodeUUID == serverUUID }
        }
        results.list()*.id
    }
}
