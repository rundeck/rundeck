package rundeck

import grails.converters.JSON
import groovy.json.JsonSlurper

/**
 * Snapshot of job execution metrics for fast retrieval.
 * Contains pre-computed 7-day metrics + today's incremental counters.
 */
class JobMetricsSnapshot {

    Long jobId  // FK to ScheduledExecution, but not a domain relationship (for performance)
    Date snapshotDate  // When was this last fully rebuilt?

    // 7-DAY HISTORICAL TOTALS (completed days only, not including today)
    Integer total7day = 0
    Integer succeeded7day = 0
    Integer failed7day = 0
    Integer aborted7day = 0
    Integer timedout7day = 0
    Long totalDuration7day = 0L  // milliseconds
    Long minDuration7day
    Long maxDuration7day

    // DAILY BREAKDOWN (JSON)
    String dailyBreakdown  // JSON array: [{date: '2025-01-01', total: 100, succeeded: 95, failed: 5}, ...]

    // HOURLY HEATMAP (JSON)
    String hourlyHeatmap  // JSON array: [120, 130, 95, ...] (24 values)

    // TODAY'S INCREMENTAL COUNTERS (reset at midnight by nightly job)
    Integer todayTotal = 0
    Integer todaySucceeded = 0
    Integer todayFailed = 0
    Integer todayAborted = 0
    Integer todayTimedout = 0
    Long todayDuration = 0L

    // TODAY'S HOURLY BREAKDOWN (JSON)
    String todayHourly  // JSON array: [0, 0, 5, 10, 8, ...] (24 values)

    Date dateCreated
    Date lastUpdated

    static constraints = {
        jobId unique: true, nullable: false
        snapshotDate nullable: false
        dailyBreakdown nullable: true, maxSize: 4000  // ~7 days of data
        hourlyHeatmap nullable: true, maxSize: 500
        todayHourly nullable: true, maxSize: 500
        minDuration7day nullable: true
        maxDuration7day nullable: true
    }

    static mapping = {
        id generator: 'assigned', name: 'jobId'  // Use jobId as primary key
        version false  // Disable optimistic locking for performance
        cache usage: 'read-write'  // Enable second-level cache

        // Explicit column mappings (migration uses snake_case with underscores around numbers)
        jobId column: 'job_id'
        snapshotDate column: 'snapshot_date'
        total7day column: 'total_7day'
        succeeded7day column: 'succeeded_7day'
        failed7day column: 'failed_7day'
        aborted7day column: 'aborted_7day'
        timedout7day column: 'timedout_7day'
        totalDuration7day column: 'total_duration_7day'
        minDuration7day column: 'min_duration_7day'
        maxDuration7day column: 'max_duration_7day'
        dailyBreakdown column: 'daily_breakdown'
        hourlyHeatmap column: 'hourly_heatmap'
        todayTotal column: 'today_total'
        todaySucceeded column: 'today_succeeded'
        todayFailed column: 'today_failed'
        todayAborted column: 'today_aborted'
        todayTimedout column: 'today_timedout'
        todayDuration column: 'today_duration'
        todayHourly column: 'today_hourly'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }

    static transients = ['hourlyHeatmapData', 'dailyBreakdownData', 'todayHourlyData']

    // ============================================================
    // HELPER METHODS - JSON PARSING
    // ============================================================

    /**
     * Parse daily breakdown JSON to List<Map>
     */
    List<Map> getDailyBreakdownData() {
        if (!dailyBreakdown) return []
        try {
            return new JsonSlurper().parseText(dailyBreakdown) as List<Map>
        } catch (Exception e) {
            log.error("Failed to parse dailyBreakdown JSON for job ${jobId}", e)
            return []
        }
    }

    /**
     * Set daily breakdown from List<Map>
     */
    void setDailyBreakdownData(List<Map> data) {
        this.dailyBreakdown = (data as JSON).toString()
    }

    /**
     * Parse hourly heatmap JSON to List<Integer>
     */
    List<Integer> getHourlyHeatmapData() {
        if (!hourlyHeatmap) return (0..23).collect { 0 }
        try {
            return new JsonSlurper().parseText(hourlyHeatmap) as List<Integer>
        } catch (Exception e) {
            log.error("Failed to parse hourlyHeatmap JSON for job ${jobId}", e)
            return (0..23).collect { 0 }
        }
    }

    /**
     * Set hourly heatmap from List<Integer>
     */
    void setHourlyHeatmapData(List<Integer> data) {
        this.hourlyHeatmap = (data as JSON).toString()
    }

    /**
     * Parse today's hourly breakdown JSON to List<Integer>
     */
    List<Integer> getTodayHourlyData() {
        if (!todayHourly) return (0..23).collect { 0 }
        try {
            return new JsonSlurper().parseText(todayHourly) as List<Integer>
        } catch (Exception e) {
            log.error("Failed to parse todayHourly JSON for job ${jobId}", e)
            return (0..23).collect { 0 }
        }
    }

    /**
     * Set today's hourly from List<Integer>
     */
    void setTodayHourlyData(List<Integer> data) {
        this.todayHourly = (data as JSON).toString()
    }

    // ============================================================
    // BUSINESS LOGIC - COMBINING DATA
    // ============================================================

    /**
     * Get combined metrics (7-day historical + today's incremental)
     */
    Map getCombinedMetrics() {
        def total = (total7day ?: 0) + (todayTotal ?: 0)
        def succeeded = (succeeded7day ?: 0) + (todaySucceeded ?: 0)
        def failed = (failed7day ?: 0) + (todayFailed ?: 0)
        def aborted = (aborted7day ?: 0) + (todayAborted ?: 0)
        def timedout = (timedout7day ?: 0) + (todayTimedout ?: 0)
        def totalDuration = (totalDuration7day ?: 0L) + (todayDuration ?: 0L)

        def successRate = total > 0 ? (succeeded * 100.0 / total) : 0.0
        def avgDuration = total > 0 ? (totalDuration / total) : 0L

        return [
            total: total,
            succeeded: succeeded,
            failed: failed,
            aborted: aborted,
            timedout: timedout,
            successRate: successRate,
            duration: [
                average: avgDuration,
                min: minDuration7day,  // Don't combine with today (hard to track min/max incrementally)
                max: maxDuration7day
            ]
        ]
    }

    /**
     * Get combined daily breakdown (7-day historical + today)
     */
    List<Map> getCombinedDailyBreakdown() {
        def historical = getDailyBreakdownData()

        // Add today's data as the 8th element
        def today = [
            date: new Date().format('yyyy-MM-dd'),
            total: todayTotal ?: 0,
            succeeded: todaySucceeded ?: 0,
            failed: todayFailed ?: 0,
            aborted: todayAborted ?: 0,
            timedout: todayTimedout ?: 0
        ]

        return historical + [today]
    }

    /**
     * Get combined hourly heatmap (7-day historical + today)
     */
    List<Integer> getCombinedHourlyHeatmap() {
        def historical = getHourlyHeatmapData()
        def today = getTodayHourlyData()

        // Sum corresponding hours
        return (0..23).collect { hour ->
            (historical[hour] ?: 0) + (today[hour] ?: 0)
        }
    }

    // ============================================================
    // INCREMENT HELPERS (for real-time updates)
    // ============================================================

    /**
     * Increment today's counters based on execution status
     * Called by event subscriber
     */
    void incrementToday(String status, Long durationMs, Integer hour) {
        todayTotal = (todayTotal ?: 0) + 1

        switch(status) {
            case 'succeeded':
                todaySucceeded = (todaySucceeded ?: 0) + 1
                break
            case 'failed':
            case 'failed-with-retry':
                todayFailed = (todayFailed ?: 0) + 1
                break
            case 'aborted':
                todayAborted = (todayAborted ?: 0) + 1
                break
            case 'timedout':
                todayTimedout = (todayTimedout ?: 0) + 1
                break
        }

        if (durationMs != null) {
            todayDuration = (todayDuration ?: 0L) + durationMs
        }

        // Increment hourly counter
        if (hour != null && hour >= 0 && hour < 24) {
            def hourlyData = getTodayHourlyData()
            hourlyData[hour]++
            setTodayHourlyData(hourlyData)
        }
    }

    /**
     * Reset today's counters (called at midnight by nightly job)
     */
    void resetTodayCounters() {
        todayTotal = 0
        todaySucceeded = 0
        todayFailed = 0
        todayAborted = 0
        todayTimedout = 0
        todayDuration = 0L
        todayHourly = null  // Will initialize to zeros on next access
    }
}
