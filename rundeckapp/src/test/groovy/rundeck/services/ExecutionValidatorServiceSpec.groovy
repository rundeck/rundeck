package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobReference
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

class ExecutionValidatorServiceSpec extends Specification implements ServiceUnitTest<ExecutionValidatorService> {

  def setup() {
  }

  def cleanup() {
  }

  @Unroll
  void "test multiple executions validation: enabled:#multExecs, limit:#multLimit"() {
    setup:
    ExecutionValidatorService.metaClass.static.findRunningExecutions = { String jobUUID, boolean withRetry, long prevRetryId ->
      ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
    }

    when:
    JobReference job = new JobReferenceImpl()
    job.id = UUID.randomUUID().toString()
    job.project = "Test"
    job.jobName = "TestJob"
    job.groupPath = "group/path"
    job.serverUUID = UUID.randomUUID().toString()
    job.multipleExecutions = multExecs
    job.maxMultipleExecutions = multLimit
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
