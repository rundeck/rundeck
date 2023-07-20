package rundeck.services

import com.dtolabs.rundeck.core.execution.JobValidationReference
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification
import spock.lang.Unroll

class ExecutionValidatorServiceSpec extends Specification implements ServiceUnitTest<ExecutionValidatorService>, DataTest {

  @Override
  Class[] getDomainClassesToMock() {
    return [ScheduledExecution, Execution]
  }


  def setup() {
  }

  def cleanup() {
  }

  private static ScheduledExecution createScheduledExecution() {
    return new ScheduledExecution(
        jobName: 'blue',
        uuid: UUID.randomUUID().toString(),
        project: 'AProject',
        groupPath: 'some/where',
        description: 'a job',
        argString: '-a b -c d',
        serverNodeUUID: null,
        scheduled: true
    ).save()
  }

  private static void createExecutions(ScheduledExecution se, int n) {
    for (i in 0..<n) {
      def exec = new Execution(
          jobUuid:se.uuid,
          dateStarted : new Date(),
          dateCompleted: null,
          user: 'userB',
          project: 'AProject',
          status: null
      ).save()
    }
  }

  @Unroll
  void "test multiple executions validation: enabled:#multExecs, limit:#multLimit"() {
//    setup:
//    ExecutionValidatorService.metaClass.static.findRunningExecutions = { String jobUUID, boolean withRetry, long prevRetryId ->
//      ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
//    }
    given:
    ScheduledExecution se = createScheduledExecution()
    createExecutions(se, 10)

    when:
    se.multipleExecutions = multExecs
    se.maxMultipleExecutions = multLimit
    se.save()
    JobValidationReference job = ExecutionValidatorService.buildJobReference(se)
    def result = service.canRunMoreExecutions(job, false, -1)

    then:
    result == expected

    where:
    multExecs | multLimit | expected
    false     | null      | false
    true      | null      | true
    true      | 9         | false
    true      | 10        | false
    true      | 11        | true
    true      | 1         | false
    null      | null      | false

  }
}
