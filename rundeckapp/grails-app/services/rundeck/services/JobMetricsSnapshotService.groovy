package rundeck.services

import grails.gorm.transactions.Transactional
import groovy.time.TimeCategory
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.type.StandardBasicTypes
import rundeck.Execution
import rundeck.JobMetricsSnapshot
import rundeck.ScheduledExecution

import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Service for managing job metrics snapshots.
 * Handles nightly rebuilds and real-time batched updates.
 */
@Transactional
class JobMetricsSnapshotService {

    def grailsApplication
    def executorService  // Grails ExecutorService bean

    // ============================================================
    // BATCHED REAL-TIME UPDATE SYSTEM
    // ============================================================

    // In-memory queue of pending execution updates
    private final ConcurrentLinkedQueue<ExecutionUpdate> updateQueue = new ConcurrentLinkedQueue<>()

    // Track queue size for monitoring
    private final AtomicInteger queueSize = new AtomicInteger(0)

    // Lock for batch processing
    private final ReentrantLock batchLock = new ReentrantLock()

    // Configuration
    private static final int BATCH_SIZE = 100           // Flush every 100 executions
    private static final int FLUSH_INTERVAL_MS = 5000   // Flush every 5 seconds
    private static final int MAX_QUEUE_SIZE = 10000     // Alert threshold

    // Scheduled flush task
    private volatile boolean flushTaskRunning = false

    /**
     * Data class for queued execution updates
     */
    private static class ExecutionUpdate {
        Long jobId
        String status
        Long durationMs
        Integer hour
        Date timestamp

        ExecutionUpdate(Long jobId, String status, Long durationMs, Integer hour) {
            this.jobId = jobId
            this.status = status
            this.durationMs = durationMs
            this.hour = hour
            this.timestamp = new Date()
        }
    }

    /**
     * Initialize background flush task
     */
    @PostConstruct
    void initializeFlushTask() {
        if (!grailsApplication.config.getProperty('rundeck.metrics.snapshot.realtimeUpdates.enabled', Boolean, true)) {
            log.info("[METRICS-SNAPSHOT] Real-time updates disabled by configuration")
            return
        }

        log.info("[METRICS-SNAPSHOT] Starting background flush task")
        flushTaskRunning = true

        // Schedule periodic flush using Grails ExecutorService
        executorService.submit {
            while (flushTaskRunning) {
                try {
                    Thread.sleep(FLUSH_INTERVAL_MS)
                    if (queueSize.get() > 0) {
                        log.debug("[METRICS-SNAPSHOT] Periodic flush triggered: ${queueSize.get()} items in queue")
                        flushBatchedUpdates()
                    }
                } catch (InterruptedException e) {
                    log.warn("[METRICS-SNAPSHOT] Flush task interrupted")
                    Thread.currentThread().interrupt()
                    break
                } catch (Exception e) {
                    log.error("[METRICS-SNAPSHOT] Error in flush task", e)
                    // Continue running despite errors
                }
            }
            log.info("[METRICS-SNAPSHOT] Flush task stopped")
        }
    }

    /**
     * Shutdown hook
     */
    @PreDestroy
    void shutdown() {
        log.info("[METRICS-SNAPSHOT] Shutting down, flushing remaining updates")
        flushTaskRunning = false

        // Flush any remaining updates
        if (queueSize.get() > 0) {
            flushBatchedUpdates()
        }
    }

    // ============================================================
    // NIGHTLY REBUILD (runs at 2:00 AM)
    // ============================================================

    /**
     * Rebuild all job metrics snapshots from execution table.
     * This is the "source of truth" recalculation that runs nightly.
     *
     * Can take hours - that's OK, runs off-peak.
     */
    def rebuildAllSnapshots() {
        log.info("[METRICS-SNAPSHOT] Starting nightly rebuild of all job snapshots")
        def startTime = System.currentTimeMillis()

        // Calculate date range: 7 completed days (not including today)
        def today = LocalDate.now()
        def endDate = today.minusDays(1)  // Yesterday
        def startDate = endDate.minusDays(6)  // 7 days ago

        log.info("[METRICS-SNAPSHOT] Rebuilding for date range: ${startDate} to ${endDate}")

        // Get all jobs
        def jobs = ScheduledExecution.list()
        log.info("[METRICS-SNAPSHOT] Found ${jobs.size()} jobs to process")

        def processed = 0
        def errors = 0

        jobs.each { ScheduledExecution job ->
            try {
                rebuildSnapshotForJob(job, startDate, endDate)
                processed++

                if (processed % 100 == 0) {
                    log.info("[METRICS-SNAPSHOT] Progress: ${processed}/${jobs.size()} jobs processed")
                }
            } catch (Exception e) {
                log.error("[METRICS-SNAPSHOT] Failed to rebuild snapshot for job ${job.id}: ${job.jobName}", e)
                errors++
            }
        }

        def duration = System.currentTimeMillis() - startTime
        log.info("[METRICS-SNAPSHOT] Nightly rebuild completed: ${processed} jobs processed, ${errors} errors, ${duration}ms total")

        return [
            processed: processed,
            errors: errors,
            duration: duration
        ]
    }

    /**
     * Rebuild snapshot for a single job.
     * Queries execution table for 7-day window and recalculates everything.
     */
    private def rebuildSnapshotForJob(ScheduledExecution job, LocalDate startDate, LocalDate endDate) {
        log.debug("[METRICS-SNAPSHOT] Rebuilding snapshot for job ${job.id}: ${job.jobName}")

        // Convert LocalDate to Date for database queries
        def startDateTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        def endDateTime = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

        // Query: Get all execution data (ONE query, all fields we need)
        def executions = Execution.createCriteria().list {
            eq('scheduledExecution', job)
            between('dateStarted', startDateTime, endDateTime)
            isNotNull('dateCompleted')
            projections {
                property('dateStarted')
                property('dateCompleted')
                property('status')
            }
        }

        // Calculate all metrics from execution data (in-memory, no more DB queries)
        def metrics = calculateMetricsFromExecutions(executions)

        // Find or create snapshot
        def snapshot = JobMetricsSnapshot.get(job.id)
        if (!snapshot) {
            snapshot = new JobMetricsSnapshot(jobId: job.id)
            log.debug("[METRICS-SNAPSHOT] Creating new snapshot for job ${job.id}")
        }

        // Update snapshot with fresh data
        snapshot.snapshotDate = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        snapshot.total7day = metrics.total
        snapshot.succeeded7day = metrics.succeeded
        snapshot.failed7day = metrics.failed
        snapshot.aborted7day = metrics.aborted
        snapshot.timedout7day = metrics.timedout
        snapshot.totalDuration7day = metrics.totalDuration
        snapshot.minDuration7day = metrics.minDuration
        snapshot.maxDuration7day = metrics.maxDuration

        snapshot.setDailyBreakdownData(metrics.dailyBreakdown)
        snapshot.setHourlyHeatmapData(metrics.hourlyHeatmap)

        // Reset today's counters (it's midnight, new day starting)
        snapshot.resetTodayCounters()

        snapshot.save(flush: false, failOnError: true)

        log.debug("[METRICS-SNAPSHOT] Snapshot rebuilt for job ${job.id}: ${metrics.total} executions in 7 days")
    }

    /**
     * Calculate all metrics from execution data (in-memory).
     * ONE pass through the data, calculate everything.
     */
    private Map calculateMetricsFromExecutions(List executions) {
        def total = 0
        def succeeded = 0
        def failed = 0
        def aborted = 0
        def timedout = 0

        def durations = []
        def byDate = [:].withDefault { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }
        def hourCounts = (0..23).collectEntries { [it, 0] }

        executions.each { row ->
            def dateStarted = row[0] as Date
            def dateCompleted = row[1] as Date
            def status = row[2] as String

            // Count totals
            total++

            // Count by status
            switch(status) {
                case 'succeeded':
                    succeeded++
                    break
                case 'failed':
                case 'failed-with-retry':
                    failed++
                    break
                case 'aborted':
                    aborted++
                    break
                case 'timedout':
                    timedout++
                    break
            }

            // Calculate duration
            def durationMs = dateCompleted.time - dateStarted.time
            durations.add(durationMs)

            // Daily breakdown
            def dateKey = dateStarted.format('yyyy-MM-dd')
            byDate[dateKey].total++
            switch(status) {
                case 'succeeded':
                    byDate[dateKey].succeeded++
                    break
                case 'failed':
                case 'failed-with-retry':
                    byDate[dateKey].failed++
                    break
                case 'aborted':
                    byDate[dateKey].aborted++
                    break
                case 'timedout':
                    byDate[dateKey].timedout++
                    break
            }

            // Hourly heatmap
            def hour = dateStarted.hours
            hourCounts[hour]++
        }

        // Calculate duration stats
        def totalDuration = durations ? durations.sum() as Long : 0L
        def minDuration = durations ? durations.min() as Long : null
        def maxDuration = durations ? durations.max() as Long : null

        // Format daily breakdown
        def dailyBreakdown = byDate.collect { dateKey, counts ->
            [date: dateKey] + counts
        }.sort { it.date }

        // Format hourly heatmap
        def hourlyHeatmap = hourCounts.values() as List<Integer>

        return [
            total: total,
            succeeded: succeeded,
            failed: failed,
            aborted: aborted,
            timedout: timedout,
            totalDuration: totalDuration,
            minDuration: minDuration,
            maxDuration: maxDuration,
            dailyBreakdown: dailyBreakdown,
            hourlyHeatmap: hourlyHeatmap
        ]
    }

    // ============================================================
    // MANUAL REBUILD (for testing / recovery)
    // ============================================================

    /**
     * Rebuild snapshot for a single job (by job ID).
     * Useful for testing or manual recovery.
     */
    def rebuildSnapshotForJobId(Long jobId) {
        def job = ScheduledExecution.get(jobId)
        if (!job) {
            throw new IllegalArgumentException("Job not found: ${jobId}")
        }

        def today = LocalDate.now()
        def endDate = today.minusDays(1)
        def startDate = endDate.minusDays(6)

        rebuildSnapshotForJob(job, startDate, endDate)

        log.info("[METRICS-SNAPSHOT] Manual rebuild completed for job ${jobId}")
    }

    /**
     * Delete and rebuild snapshot for a job (full reset).
     */
    def resetSnapshotForJobId(Long jobId) {
        def snapshot = JobMetricsSnapshot.get(jobId)
        if (snapshot) {
            snapshot.delete(flush: true)
            log.info("[METRICS-SNAPSHOT] Deleted snapshot for job ${jobId}")
        }

        rebuildSnapshotForJobId(jobId)
    }

    // ============================================================
    // QUEUE EXECUTION UPDATES (called by event subscriber)
    // ============================================================

    /**
     * Queue an execution update for batched processing.
     * This is called by the ExecutionCompleteEvent subscriber.
     *
     * FAST: Just adds to in-memory queue, no database I/O.
     */
    def queueExecutionUpdate(Execution execution) {
        if (!execution?.scheduledExecution?.id) {
            log.warn("[METRICS-SNAPSHOT] Skipping execution update: no job ID")
            return
        }

        def jobId = execution.scheduledExecution.id
        def status = execution.status
        def durationMs = execution.dateCompleted && execution.dateStarted ?
            (execution.dateCompleted.time - execution.dateStarted.time) : null
        def hour = execution.dateStarted?.hours  // 0-23

        // Add to queue
        def update = new ExecutionUpdate(jobId, status, durationMs, hour)
        updateQueue.offer(update)
        def currentSize = queueSize.incrementAndGet()

        log.trace("[METRICS-SNAPSHOT] Queued update for job ${jobId}: ${status}, queue size: ${currentSize}")

        // Check for queue overflow
        if (currentSize > MAX_QUEUE_SIZE) {
            log.warn("[METRICS-SNAPSHOT] Queue size exceeded threshold: ${currentSize} > ${MAX_QUEUE_SIZE}")
            // TODO: Alert ops team
        }

        // Trigger immediate flush if batch size reached
        if (currentSize >= BATCH_SIZE) {
            log.debug("[METRICS-SNAPSHOT] Batch size reached: ${currentSize}, triggering flush")
            // Use async to not block execution completion
            executorService.submit { flushBatchedUpdates() }
        }
    }

    // ============================================================
    // BATCH FLUSH LOGIC
    // ============================================================

    /**
     * Flush all queued updates to database in a single transaction.
     * Groups updates by job ID for efficient batch updates.
     */
    @Transactional
    private void flushBatchedUpdates() {
        // Prevent concurrent flushes
        if (!batchLock.tryLock()) {
            log.debug("[METRICS-SNAPSHOT] Flush already in progress, skipping")
            return
        }

        try {
            def batchStartTime = System.currentTimeMillis()
            def updates = []
            def update

            // Drain queue (up to BATCH_SIZE * 2 to avoid starvation)
            def maxDrain = BATCH_SIZE * 2
            while ((update = updateQueue.poll()) != null && updates.size() < maxDrain) {
                updates.add(update)
                queueSize.decrementAndGet()
            }

            if (updates.isEmpty()) {
                return
            }

            log.debug("[METRICS-SNAPSHOT] Flushing ${updates.size()} updates")

            // Group updates by job ID
            def byJobId = updates.groupBy { it.jobId }

            def processed = 0
            def errors = 0

            byJobId.each { jobId, jobUpdates ->
                try {
                    applyBatchedUpdatesToJob(jobId, jobUpdates)
                    processed++
                } catch (Exception e) {
                    log.error("[METRICS-SNAPSHOT] Failed to apply updates for job ${jobId}", e)
                    errors++
                }
            }

            def duration = System.currentTimeMillis() - batchStartTime
            log.info("[METRICS-SNAPSHOT] Batch flush completed: ${updates.size()} updates, " +
                     "${processed} jobs updated, ${errors} errors, ${duration}ms")

        } finally {
            batchLock.unlock()
        }
    }

    /**
     * Apply batched updates to a single job's snapshot.
     * Aggregates multiple updates into single database write.
     */
    @Transactional
    private void applyBatchedUpdatesToJob(Long jobId, List<ExecutionUpdate> updates) {
        // Find or create snapshot
        def snapshot = JobMetricsSnapshot.get(jobId)
        if (!snapshot) {
            log.debug("[METRICS-SNAPSHOT] Creating new snapshot for job ${jobId} (from batch update)")
            snapshot = new JobMetricsSnapshot(
                jobId: jobId,
                snapshotDate: new Date() - 1  // Yesterday (will be rebuilt tonight)
            )
        }

        // Aggregate all updates
        def todayTotal = 0
        def todaySucceeded = 0
        def todayFailed = 0
        def todayAborted = 0
        def todayTimedout = 0
        def todayDuration = 0L
        def hourCounts = [:].withDefault { 0 }

        updates.each { update ->
            todayTotal++

            switch(update.status) {
                case 'succeeded':
                    todaySucceeded++
                    break
                case 'failed':
                case 'failed-with-retry':
                    todayFailed++
                    break
                case 'aborted':
                    todayAborted++
                    break
                case 'timedout':
                    todayTimedout++
                    break
            }

            if (update.durationMs) {
                todayDuration += update.durationMs
            }

            if (update.hour != null) {
                hourCounts[update.hour]++
            }
        }

        // Apply aggregated updates to snapshot
        snapshot.todayTotal = (snapshot.todayTotal ?: 0) + todayTotal
        snapshot.todaySucceeded = (snapshot.todaySucceeded ?: 0) + todaySucceeded
        snapshot.todayFailed = (snapshot.todayFailed ?: 0) + todayFailed
        snapshot.todayAborted = (snapshot.todayAborted ?: 0) + todayAborted
        snapshot.todayTimedout = (snapshot.todayTimedout ?: 0) + todayTimedout
        snapshot.todayDuration = (snapshot.todayDuration ?: 0L) + todayDuration

        // Update hourly breakdown
        def hourlyData = snapshot.getTodayHourlyData()
        hourCounts.each { hour, count ->
            if (hour >= 0 && hour < 24) {
                hourlyData[hour] = (hourlyData[hour] ?: 0) + count
            }
        }
        snapshot.setTodayHourlyData(hourlyData)

        // Save (batched, will flush at end of transaction)
        snapshot.save(flush: false, failOnError: true)

        log.trace("[METRICS-SNAPSHOT] Applied ${updates.size()} updates to job ${jobId}")
    }

    // ============================================================
    // MONITORING / ADMIN
    // ============================================================

    /**
     * Get current queue metrics (for monitoring dashboard)
     */
    Map getQueueMetrics() {
        return [
            queueSize: queueSize.get(),
            maxQueueSize: MAX_QUEUE_SIZE,
            batchSize: BATCH_SIZE,
            flushIntervalMs: FLUSH_INTERVAL_MS,
            queueUtilization: (queueSize.get() * 100.0 / MAX_QUEUE_SIZE),
            flushTaskRunning: flushTaskRunning
        ]
    }

    /**
     * Force immediate flush (for testing / admin operations)
     */
    def forceFlush() {
        log.info("[METRICS-SNAPSHOT] Manual flush requested")
        flushBatchedUpdates()
    }
}
