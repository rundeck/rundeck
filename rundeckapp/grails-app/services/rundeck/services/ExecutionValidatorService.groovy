package rundeck.services

import com.dtolabs.rundeck.core.execution.ExecutionValidator
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.events.annotation.Subscriber
import rundeck.Execution

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
      JobReference jobReference,
      boolean retry = false,
      long prevId = -1) {

    def maxExecutions = 1
    if (jobReference.multipleExecutions) {
      maxExecutions = 0
      if (jobReference.maxMultipleExecutions) {
        maxExecutions = jobReference.maxMultipleExecutions?.toInteger()
      }
    }

    if (maxExecutions > 0) {
      synchronized (syncForJob(jobReference.id)) {
        //find any currently running executions for this job, and if so, throw exception
        def found = findRunningExecutions(jobReference.id, retry, prevId)
        if (found && found.size() >= maxExecutions) {
          return false
        }
      }
    }
    return true
  }

  protected static Object findRunningExecutions(String jobUUID, boolean withRetry, long prevRetryId) {
    return Execution.withCriteria {
      isNull('dateCompleted')
      scheduledExecution {
        eq('uuid', jobUUID)
      }
      isNotNull('dateStarted')
      if (withRetry) {
        ne('id', prevRetryId)
      }
    }
  }

}
