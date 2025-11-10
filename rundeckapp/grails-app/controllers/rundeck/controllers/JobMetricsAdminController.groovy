package rundeck.controllers

import grails.converters.JSON

/**
 * TEMPORARY ADMIN CONTROLLER FOR RUN-3768 TESTING
 * TODO: DELETE THIS CONTROLLER BEFORE PRODUCTION DEPLOYMENT
 *
 * This controller provides admin endpoints to manually trigger job metrics operations
 * for testing and validation purposes during development.
 */
class JobMetricsAdminController {

    def jobMetricsSnapshotService

    /**
     * Manually trigger the nightly rebuild job.
     *
     * Usage:
     *   curl http://localhost:4440/admin/jobMetrics/rebuildAll
     *
     * Returns:
     *   JSON with rebuild results (processed count, errors, duration)
     */
    def rebuildAll() {
        log.info("[METRICS-ADMIN] Manual rebuild triggered via admin endpoint")

        try {
            def startTime = System.currentTimeMillis()
            def result = jobMetricsSnapshotService.rebuildAllSnapshots()
            def totalTime = System.currentTimeMillis() - startTime

            def response = [
                success: true,
                message: "Nightly rebuild completed successfully",
                results: [
                    jobsProcessed: result.processed,
                    errors: result.errors,
                    serviceDuration: result.duration,
                    totalRequestDuration: totalTime
                ]
            ]

            log.info("[METRICS-ADMIN] Rebuild completed: ${result.processed} jobs, ${result.errors} errors, ${totalTime}ms")

            render response as JSON

        } catch (Exception e) {
            log.error("[METRICS-ADMIN] Rebuild failed", e)

            response.status = 500
            render([
                success: false,
                message: "Rebuild failed: ${e.message}",
                error: e.toString()
            ] as JSON)
        }
    }

    /**
     * Get current queue metrics for monitoring.
     *
     * Usage:
     *   curl http://localhost:4440/admin/jobMetrics/queueStatus
     */
    def queueStatus() {
        def metrics = jobMetricsSnapshotService.getQueueMetrics()

        render([
            success: true,
            metrics: metrics
        ] as JSON)
    }

    /**
     * Force flush of batched updates.
     *
     * Usage:
     *   curl http://localhost:4440/admin/jobMetrics/forceFlush
     */
    def forceFlush() {
        log.info("[METRICS-ADMIN] Manual flush triggered via admin endpoint")

        try {
            jobMetricsSnapshotService.forceFlush()

            render([
                success: true,
                message: "Flush completed"
            ] as JSON)

        } catch (Exception e) {
            log.error("[METRICS-ADMIN] Flush failed", e)

            response.status = 500
            render([
                success: false,
                message: "Flush failed: ${e.message}",
                error: e.toString()
            ] as JSON)
        }
    }

    /**
     * Get snapshot statistics.
     *
     * Usage:
     *   curl http://localhost:4440/admin/jobMetrics/stats
     */
    def stats() {
        try {
            def totalSnapshots = rundeck.JobMetricsSnapshot.count()
            def totalJobs = rundeck.ScheduledExecution.count()

            def snapshots = rundeck.JobMetricsSnapshot.list(max: 10, sort: 'lastUpdated', order: 'desc')

            def sampleData = snapshots.collect { snapshot ->
                [
                    jobId: snapshot.jobId,
                    snapshotDate: snapshot.snapshotDate,
                    total7day: snapshot.total7day,
                    succeeded7day: snapshot.succeeded7day,
                    failed7day: snapshot.failed7day,
                    todayTotal: snapshot.todayTotal,
                    lastUpdated: snapshot.lastUpdated
                ]
            }

            render([
                success: true,
                stats: [
                    totalSnapshots: totalSnapshots,
                    totalJobs: totalJobs,
                    coverage: totalJobs > 0 ? ((totalSnapshots * 100.0) / totalJobs).round(2) : 0
                ],
                sampleSnapshots: sampleData
            ] as JSON)

        } catch (Exception e) {
            log.error("[METRICS-ADMIN] Stats query failed", e)

            response.status = 500
            render([
                success: false,
                message: "Stats query failed: ${e.message}",
                error: e.toString()
            ] as JSON)
        }
    }

    /**
     * Rebuild snapshot for a specific job.
     *
     * Usage:
     *   curl http://localhost:4440/admin/jobMetrics/rebuildJob?jobId=123
     */
    def rebuildJob() {
        def jobId = params.jobId as Long

        if (!jobId) {
            response.status = 400
            render([
                success: false,
                message: "Missing required parameter: jobId"
            ] as JSON)
            return
        }

        log.info("[METRICS-ADMIN] Manual rebuild triggered for job ${jobId}")

        try {
            jobMetricsSnapshotService.rebuildSnapshotForJobId(jobId)

            render([
                success: true,
                message: "Snapshot rebuilt for job ${jobId}"
            ] as JSON)

        } catch (IllegalArgumentException e) {
            response.status = 404
            render([
                success: false,
                message: e.message
            ] as JSON)

        } catch (Exception e) {
            log.error("[METRICS-ADMIN] Rebuild failed for job ${jobId}", e)

            response.status = 500
            render([
                success: false,
                message: "Rebuild failed: ${e.message}",
                error: e.toString()
            ] as JSON)
        }
    }
}