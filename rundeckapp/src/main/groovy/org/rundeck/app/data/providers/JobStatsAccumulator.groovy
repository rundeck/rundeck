package org.rundeck.app.data.providers

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.TaskScheduler

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Buffers job-stat updates in memory and flushes one save per job on a fixed interval.
 * A job that is already being saved is left alone: arrivals during that save stay queued
 * for the next interval, so the same row is never written twice at the same time.
 */
@Slf4j
class JobStatsAccumulator implements InitializingBean {

    GormJobStatsDataProvider jobStatsDataProvider
    TaskScheduler taskScheduler
    long flushIntervalMillis = 5000L

    private final Map<String, Deque<JobExecutionResult>> pending = new ConcurrentHashMap<>()
    private final Map<String, AtomicBoolean> flushing = new ConcurrentHashMap<>()

    /**
     * Queues one completed execution for {@code jobUuid}. Does not touch the database.
     * Arrivals while that job is being saved stay in the queue for the next flush.
     *
     * @param jobUuid job whose stats row will be updated
     * @param execution completion to fold into the stats; ignored when null
     */
    void add(String jobUuid, JobExecutionResult execution) {
        if (!jobUuid || execution == null) {
            return
        }
        pending.compute(jobUuid) { String uuid, Deque<JobExecutionResult> queue ->
            def target = queue ?: new ConcurrentLinkedDeque<JobExecutionResult>()
            target.addLast(execution)
            return target
        }
    }

    /**
     * Drains every queued job and saves each one once.
     * A job that is already saving is skipped; its newer executions remain queued.
     */
    void flush() {
        new ArrayList<String>(pending.keySet()).each { String jobUuid ->
            try {
                flushJob(jobUuid)
            } catch (Throwable t) {
                log.error("Failed to flush job stats for ${jobUuid}", t)
            }
        }
    }

    /**
     * Schedules {@link #flush()} at {@link #flushIntervalMillis} when a {@link TaskScheduler} is set.
     */
    @Override
    void afterPropertiesSet() {
        if (!taskScheduler) {
            return
        }
        long interval = Math.max(flushIntervalMillis, 1L)
        taskScheduler.scheduleAtFixedRate({ flush() }, interval)
    }

    /**
     * Saves the executions currently queued for one job.
     * Returns immediately when a save for that job is already in progress.
     * On failure, the drained batch is put back at the front of the queue.
     *
     * @param jobUuid job to flush
     */
    private void flushJob(String jobUuid) {
        def gate = flushing.computeIfAbsent(jobUuid) { new AtomicBoolean(false) }
        if (!gate.compareAndSet(false, true)) {
            return
        }
        try {
            def batch = drain(jobUuid)
            if (!batch) {
                return
            }
            def saved = false
            try {
                saved = jobStatsDataProvider.updateJobStats(jobUuid, batch)
            } catch (Throwable t) {
                log.warn("Job stats flush failed for ${jobUuid}, executions ${batch*.executionId}", t)
            }
            if (!saved) {
                requeueFront(jobUuid, batch)
            }
        } finally {
            gate.set(false)
        }
    }

    /**
     * Removes and returns every execution currently queued for the job, in arrival order.
     * The queue entry is dropped when it ends up empty.
     *
     * @param jobUuid job whose queue is drained
     * @return the drained executions, or an empty list when the job has nothing queued
     */
    private List<JobExecutionResult> drain(String jobUuid) {
        List<JobExecutionResult> batch = []
        pending.compute(jobUuid) { String uuid, Deque<JobExecutionResult> queue ->
            if (queue == null) {
                return null
            }
            JobExecutionResult item
            while ((item = queue.pollFirst()) != null) {
                batch << item
            }
            return queue.isEmpty() ? null : queue
        }
        return batch
    }

    /**
     * Puts a batch back at the head of the job's queue, preserving its order and leaving
     * executions that arrived during the failed save behind it.
     *
     * @param jobUuid job whose queue receives the batch
     * @param batch executions to retry, in the order they should be replayed
     */
    private void requeueFront(String jobUuid, List<JobExecutionResult> batch) {
        pending.compute(jobUuid) { String uuid, Deque<JobExecutionResult> queue ->
            def target = queue ?: new ConcurrentLinkedDeque<JobExecutionResult>()
            for (int i = batch.size() - 1; i >= 0; i--) {
                target.addFirst(batch[i])
            }
            return target
        }
    }
}
