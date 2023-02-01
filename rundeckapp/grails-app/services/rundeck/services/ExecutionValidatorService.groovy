package rundeck.services

import com.dtolabs.rundeck.core.execution.ExecutionValidator
import com.dtolabs.rundeck.core.execution.JobValidationReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.events.annotation.Subscriber
import org.rundeck.app.data.model.v1.job.JobData
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.data.util.JobDataUtil

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Service for validation of some execution properties.
 *
 */
class ExecutionValidatorService implements ExecutionValidator {

  /**
   * sync objects for preventing multiple executions of a job within this server
   */
  ConcurrentMap<String, Object> multijobflag = new ConcurrentHashMap<String, Object>()

  /**
   * Return an object for synchronization of the job id
   * @param id
   * @return object
   */
  private Object syncForJob(String id) {
    def object = new Object()
    //return existing object if present, or the new object
    multijobflag.putIfAbsent(id, object) ?: object
  }

  @Subscriber
  def jobChanged(StoredJobChangeEvent e) {
    if (e.eventType == JobChangeEvent.JobChangeEventType.DELETE) {
      //clear multijob sync object
      multijobflag?.remove(e.originalJobReference.id)
    }
  }


  /**
   * Validates if a job can run more executions.
   * @param jobReference
   * @param retry
   * @param prevId
   * @return
   */
  public boolean canRunMoreExecutions(
      JobValidationReference jobReference,
      boolean withRetry = false,
      long prevRetryId = -1) {

    def maxExecutions = 1
    if (jobReference.multipleExecutions) {
      maxExecutions = 0
      if (jobReference.maxMultipleExecutions) {
        maxExecutions = jobReference.maxMultipleExecutions
      }
    }

    if (maxExecutions > 0) {
      synchronized (syncForJob(jobReference.id)) {
        //find any currently running executions for this job, and if so, throw exception
        def found = Execution.createCriteria().get {
          projections {
            count()
          }
          isNull('dateCompleted')
          scheduledExecution {
            eq('id', jobReference.databaseId)
          }
          isNotNull('dateStarted')
          if (withRetry) {
            ne('id', prevRetryId)
          }
        }

        if (found && found >= maxExecutions) {
          return false
        }
      }
    }
    return true
  }

  /**
   * Builds a new job validation reference implementation.
   */
  public static JobValidationReference buildJobReference(final JobData se) {
    return new JobValidationReferenceImpl(
        id: JobDataUtil.getExtId(se),
        databaseId: se.id,
        uuid: se.uuid,
        project: se.project,
        jobName: se.jobName,
        groupPath: se.groupPath,
        serverUUID: se.serverNodeUUID,
        hasSecureOptions: se.hasSecureOptions(),
        multipleExecutions: se.multipleExecutions,
        maxMultipleExecutions: se.maxMultipleExecutions?.isInteger() ? Integer.parseInt(se.maxMultipleExecutions) : null

    )
  }

  static class JobValidationReferenceImpl implements JobValidationReference {
    Serializable databaseId
    String uuid
    Boolean multipleExecutions
    Integer maxMultipleExecutions
    boolean hasSecureOptions
    String project
    String id
    String jobName
    String groupPath
    String serverUUID

    @Override
    boolean hasSecureOptions() {
      return hasSecureOptions
    }
  }

}
