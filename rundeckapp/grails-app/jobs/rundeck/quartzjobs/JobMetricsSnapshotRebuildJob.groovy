package rundeck.quartzjobs

import com.dtolabs.rundeck.server.jobs.NonConcurrentJob
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnschedulableException

/**
 * Quartz job that triggers nightly rebuild of job metrics snapshots.
 * Runs at 2:00 AM every day.
 */
class JobMetricsSnapshotRebuildJob implements InterruptableJob, NonConcurrentJob {

    def jobMetricsSnapshotService

    static triggers = {
        // Run every day at 2:00 AM
        cron name: 'jobMetricsSnapshotRebuildTrigger', cronExpression: '0 0 2 * * ?'
    }

    void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("[METRICS-SNAPSHOT-JOB] Starting scheduled nightly rebuild")

        try {
            def result = jobMetricsSnapshotService.rebuildAllSnapshots()

            log.info("[METRICS-SNAPSHOT-JOB] Nightly rebuild completed successfully: " +
                     "${result.processed} jobs processed, ${result.errors} errors, " +
                     "${result.duration}ms total")

            // TODO: Send metrics to monitoring system (Datadog, etc)
            // TODO: Alert if errors > threshold

        } catch (Exception e) {
            log.error("[METRICS-SNAPSHOT-JOB] Nightly rebuild failed", e)

            // TODO: Send alert to ops team
            throw new JobExecutionException("Nightly snapshot rebuild failed", e)
        }
    }

    void interrupt() {
        log.warn("[METRICS-SNAPSHOT-JOB] Job interrupted")
        // Graceful shutdown if possible
    }
}
