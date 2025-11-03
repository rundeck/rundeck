package rundeck.quartzjobs

import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Quartz job that triggers nightly rebuild of job metrics snapshots.
 * Runs at 2:00 AM every day.
 */
class JobMetricsSnapshotRebuildJob implements InterruptableJob {

    static Logger logger = LoggerFactory.getLogger(JobMetricsSnapshotRebuildJob)
    def jobMetricsSnapshotService

    static triggers = {
        // Run every day at 2:00 AM
        cron name: 'jobMetricsSnapshotRebuildTrigger', cronExpression: '0 0 2 * * ?'
    }

    void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("[METRICS-SNAPSHOT-JOB] Starting scheduled nightly rebuild")

        try {
            def result = jobMetricsSnapshotService.rebuildAllSnapshots()

            logger.info("[METRICS-SNAPSHOT-JOB] Nightly rebuild completed successfully: " +
                     "${result.processed} jobs processed, ${result.errors} errors, " +
                     "${result.duration}ms total")

            // TODO: Send metrics to monitoring system (Datadog, etc)
            // TODO: Alert if errors > threshold

        } catch (Exception e) {
            logger.error("[METRICS-SNAPSHOT-JOB] Nightly rebuild failed", e)

            // TODO: Send alert to ops team
            throw new JobExecutionException("Nightly snapshot rebuild failed", e)
        }
    }

    void interrupt() {
        logger.warn("[METRICS-SNAPSHOT-JOB] Job interrupted")
        // Graceful shutdown if possible
    }
}
