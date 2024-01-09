package testhelper

import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow

class TestDomainFactory {

    static ScheduledExecution createJob(Map<String,Object> params = [:]) {
        def job = new ScheduledExecution(uuid: UUID.randomUUID().toString(), jobName: "test job", groupPath: "testgroup", project:"one")
        job.workflow = createWorkflow()
        params.each { k, v -> job[k] = v }
        return job.save(failOnError: true)
    }

    static Execution createExecution(Map<String,Object> params = [:]) {
        Execution e = new Execution()
        e.project = "test"
        e.user = "tester"
        e.dateStarted = new Date()
        e.status = 'running'
        e.workflow = createWorkflow()
        params.each { k, v -> e[k] = v }
        return e.save(failOnError: true)
    }

    static Workflow createWorkflow(Map<String, Object> params = [:]) {
        def w = new Workflow(commands: [new CommandExec(adhocRemoteString: "echo hello")])
        params.each { k, v -> w[k] = v }
        return w.save(failOnError: true)
    }
}
