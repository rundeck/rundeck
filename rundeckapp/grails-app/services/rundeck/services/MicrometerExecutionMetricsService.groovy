package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.StepNodeUsageWorkflowListener.StepNodeUsageEntry
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.core.GrailsApplication
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.rundeck.app.config.SysConfigProp
import org.rundeck.app.config.SystemConfig
import org.rundeck.app.config.SystemConfigurable
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Execution
import rundeck.ScheduledExecution

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.function.IntUnaryOperator
import java.util.function.Supplier
import java.util.function.ToDoubleFunction

/**
 * Emits dimensional (project/status) execution metrics directly on the Micrometer
 * {@link MeterRegistry}, so they appear natively on /monitoring/prometheus.
 *
 * This registers meters directly against the Micrometer registry -- it does NOT go through
 * {@link DropwizardMicrometerBridgeService}, which only mirrors pre-existing flat Dropwizard
 * meters and cannot carry tags/dimensions.
 */
@Slf4j
@CompileStatic
class MicrometerExecutionMetricsService implements SystemConfigurable {

    // NOTE: no 'rundeck.' prefix here. ConfigurationService.getBoolean(String, boolean) resolves
    // against appCfg, which is already grailsApplication.config.getProperty("rundeck", Map.class)
    // -- i.e. already scoped to the "rundeck" subtree. Passing the full "rundeck.xxx" string here
    // makes getValueFromRoot look for a nonexistent nested "rundeck" key *inside* that subtree and
    // silently fall through to the default (see ApiController's working
    // configurationService.getBoolean("metrics.monitoring.enabled", true) for the established
    // convention). The property set in rundeck-config.properties still needs the "rundeck." prefix
    // (e.g. rundeck.metrics.execution.job.dimension.enabled=true) -- only the lookup key here omits it.
    static final String JOB_DIMENSION_ENABLED_PROPERTY = 'metrics.execution.job.dimension.enabled'

    /**
     * Ascending, comma-separated upper bounds for the {@code node_bucket} tag. Same lookup-key
     * convention as {@link #JOB_DIMENSION_ENABLED_PROPERTY} above -- no 'rundeck.' prefix here,
     * but the property in rundeck-config.properties does need it
     * (rundeck.metrics.execution.step.nodeBuckets).
     * <p>
     * Configurable rather than hardcoded because the default edges mirror the example bands in
     * the consumption pricing model, where linear vs banded vs capped is still an open question.
     * Freezing an example in code would mean a release -- and for On-Prem, an upgrade cycle --
     * to change a number that is expected to move.
     */
    static final String NODE_BUCKETS_PROPERTY = 'metrics.execution.step.nodeBuckets'

    /** Reproduces the previously hardcoded bands exactly: 1, 2-10, 11-100, 100+. */
    static final String NODE_BUCKETS_DEFAULT = '1,10,100'

    /**
     * Upper limit on configured bands. Each one multiplies series count across
     * plugin_type x node_step x status x tenant, and nothing else stops a typo from asking for
     * hundreds.
     */
    static final int NODE_BUCKETS_MAX = 20

    @Autowired
    MeterRegistry meterRegistry
    ConfigurationService configurationService
    GrailsApplication grailsApplication
    FrameworkService frameworkService
    ExecutionService executionService

    // Backs the in-memory "currently running" gauge: incremented in recordExecutionStart(),
    // decremented in recordExecution() (finish). Event-driven, zero DB/scrape cost -- unlike a
    // live COUNT(*) query, this only reflects executions this JVM itself started, so cluster-wide
    // totals need summing across instances in PromQL (sum by (project) (...)).
    private final ConcurrentHashMap<List<Tag>, AtomicInteger> runningCounters = new ConcurrentHashMap<>()

    /**
     * Marks an execution as started for the "currently running" gauge. Must be paired with the
     * matching {@link #recordExecution} call when it finishes. No-op under the same conditions as
     * recordExecution.
     */
    void recordExecutionStart(Execution execution) {
        if (!meterRegistry || !execution) {
            return
        }
        runningCounterFor(runningTags(execution)).incrementAndGet()
    }

    /**
     * Records a finished execution as a tagged counter increment and duration timer, and
     * decrements the matching "currently running" gauge.
     * No-op when the registry is unavailable or the execution is null.
     */
    void recordExecution(Execution execution) {
        if (!meterRegistry || !execution) {
            return
        }

        String project = execution.project
        String status = execution.getExecutionState()
        // job_id/job_name are only added for scheduled (non-ad-hoc) executions -- ad-hoc runs have
        // no stable job identity worth tagging, and are deliberately excluded from this dimension
        // entirely rather than given a sentinel value.
        List<Tag> jobTags = jobTags(execution)

        List<Tag> tags = [Tag.of('project', project), Tag.of('status', status)]
        tags.addAll(jobTags)

        meterRegistry.counter('rundeck.executions', tags).increment()

        Date started = execution.dateStarted
        Date completed = execution.dateCompleted
        if (started && completed) {
            long millis = completed.time - started.time
            durationTimer(tags).record(millis, TimeUnit.MILLISECONDS)
        }

        AtomicInteger runningCounter = runningCounters.get(runningTagsOf(project, jobTags))
        runningCounter?.updateAndGet({ int v -> Math.max(0, v - 1) } as IntUnaryOperator)
    }

    /**
     * Records the step_node_seconds total (sum of per-step, per-node durations across the
     * execution tree, including nested job-reference calls) as its own timer, tagged the same
     * way as {@link #recordExecution}'s duration timer. A separate meter, not folded into
     * rundeck.execution.duration, since it's a materially different figure -- summed across
     * nodes, not wall-clock -- and mixing the two would corrupt that meter's _sum/_avg/_max.
     * No-op when the registry/execution is unavailable, or when no total was recorded for this
     * execution (e.g. it crashed before finishing).
     */
    void recordStepNodeSeconds(Execution execution, Long stepNodeSeconds) {
        if (!meterRegistry || !execution || stepNodeSeconds == null) {
            return
        }
        List<Tag> tags = [Tag.of('project', execution.project), Tag.of('status', execution.getExecutionState())]
        tags.addAll(jobTags(execution))
        stepNodeSecondsTimer(tags).record(stepNodeSeconds, TimeUnit.SECONDS)
    }

    /**
     * Records the step_node_count total (sum of per-step, per-node dispatch counts across the
     * execution tree, including nested job-reference calls -- {@code StepNodeUsageEntry.nodeCount})
     * as its own distribution summary, tagged the same way as
     * {@link #recordStepNodeSeconds}. A {@link DistributionSummary}, not a {@link Timer}, since
     * this is a discrete count, not a duration -- using a Timer would report it in a time unit it
     * isn't. No-op when the registry/execution is unavailable, or when no total was recorded for
     * this execution (e.g. it crashed before finishing).
     */
    void recordStepNodeCount(Execution execution, Long stepNodeCount) {
        if (!meterRegistry || !execution || stepNodeCount == null) {
            return
        }
        List<Tag> tags = [Tag.of('project', execution.project), Tag.of('status', execution.getExecutionState())]
        tags.addAll(jobTags(execution))
        stepNodeCountSummary(tags).record(stepNodeCount)
    }

    /**
     * Records one {@code rundeck.execution.step.nodes} and one
     * {@code rundeck.execution.step.duration} sample per step in the breakdown, tagged with the
     * step's plugin type and a bucketed node-dispatch count. This is the per-step counterpart to
     * {@link #recordStepNodeCount}/{@link #recordStepNodeSeconds}, which only see the already
     * summed per-execution totals and so cannot answer "which plugin types ran, against how many
     * nodes".
     * <p>
     * <b>Deliberately carries no {@code project} tag</b> (and no job_id/job_name), unlike every
     * other meter in this service. These are emitted per step, so the series count is already
     * multiplied by the plugin catalogue and the bucket set; adding project would multiply it
     * again by a per-tenant project count that nothing bounds. The question these answer is asked
     * per tenant, and the tenant is already identifiable from the scrape target, so a project
     * dimension would cost cardinality without earning an answer. For the same reason they are
     * absent from {@code onJobChanged}'s cleanup list -- carrying no job_id, they create no
     * per-job series to go stale.
     * <p>
     * No-op when the registry is unavailable, the execution is null, or no breakdown was recorded
     * for it (e.g. it crashed before finishing).
     */
    void recordStepUsage(Execution execution, Map<String, StepNodeUsageEntry> breakdown) {
        if (!meterRegistry || !execution || breakdown == null) {
            return
        }
        String status = execution.getExecutionState()
        for (StepNodeUsageEntry entry : breakdown.values()) {
            Boolean nodeStep = entry.getIsNodeStep()
            List<Tag> tags = [
                Tag.of('plugin_type', entry.getPluginType() ?: 'unknown'),
                Tag.of('node_step', nodeStep == null ? 'unknown' : nodeStep.toString()),
                Tag.of('node_bucket', nodeBucket(entry.getNodeCount())),
                Tag.of('status', status)
            ]
            stepNodesSummary(tags).record(entry.getNodeCount() as double)
            // Recorded at nanosecond precision rather than pre-rounded to whole seconds: a single
            // step's duration is routinely sub-second, and rounding each sample before recording
            // would floor most of them to zero. The Timer accumulates at full precision and the
            // scrape still exposes seconds.
            stepDurationTimer(tags).record(entry.getNanos(), TimeUnit.NANOSECONDS)
        }
    }

    /**
     * Buckets a step's node-dispatch count into the configured band set, so {@code node_bucket}
     * contributes a constant factor to cardinality no matter how wide a step fans out.
     * <p>
     * {@code 0} is not reachable from today's {@code StepNodeUsageWorkflowListener}: every path
     * that adds elapsed time also increments the node count, so a step that dispatched to no
     * nodes still reports 1 (it contributes its own duration once). Non-dispatching steps are
     * therefore told apart by the {@code node_step} tag, not by this bucket. The zero case is
     * kept so that a future listener change cannot silently produce an unlabelled bucket.
     */
    private String nodeBucket(long nodeCount) {
        if (nodeCount <= 0L) {
            return '0'
        }
        NodeBucketScheme scheme = nodeBucketScheme()
        List<Long> bounds = scheme.bounds
        for (int i = 0; i < bounds.size(); i++) {
            if (nodeCount <= bounds.get(i)) {
                return scheme.labels.get(i)
            }
        }
        scheme.labels.get(bounds.size())
    }

    /**
     * The resolved band edges and the labels derived from them, built once and cached.
     * {@link #nodeBucket} runs once per step of every execution, so the property string is
     * parsed on startup rather than on that path.
     */
    @CompileStatic
    private static final class NodeBucketScheme {
        final List<Long> bounds
        final List<String> labels

        NodeBucketScheme(final List<Long> bounds, final List<String> labels) {
            this.bounds = Collections.unmodifiableList(new ArrayList<Long>(bounds))
            this.labels = Collections.unmodifiableList(new ArrayList<String>(labels))
        }
    }

    private volatile NodeBucketScheme resolvedNodeBucketScheme

    /**
     * Resolves {@link #NODE_BUCKETS_PROPERTY} once and caches it. Double-checked on a volatile
     * field: this is read on a per-step hot path, and the alternative of resolving in
     * {@link #initialize} would leave the bands unset for any execution that completed before
     * the bootstrap event fired.
     * <p>
     * Bad configuration never propagates: anything unparseable, non-ascending, non-positive or
     * over {@link #NODE_BUCKETS_MAX} falls back to the default and logs. A metrics path must
     * not be the thing that breaks an execution, the same line
     * {@code StepNodeUsageWorkflowListener} holds.
     */
    private NodeBucketScheme nodeBucketScheme() {
        NodeBucketScheme cached = resolvedNodeBucketScheme
        if (cached != null) {
            return cached
        }
        synchronized (this) {
            if (resolvedNodeBucketScheme != null) {
                return resolvedNodeBucketScheme
            }
            String raw = configurationService != null ?
                configurationService.getString(NODE_BUCKETS_PROPERTY, NODE_BUCKETS_DEFAULT) :
                NODE_BUCKETS_DEFAULT
            List<Long> bounds = null
            // A null means "not configured" (no ConfigurationService, or a stub that returns
            // nothing) and quietly takes the default. An explicitly-set but unusable value is a
            // different thing and is worth warning about.
            if (raw != null) {
                bounds = parseNodeBucketBounds(raw)
                if (bounds == null) {
                    log.warn(
                        "Ignoring rundeck.{}={} -- expected ascending, comma-separated integers >= 1. Using default '{}'.",
                        NODE_BUCKETS_PROPERTY, raw, NODE_BUCKETS_DEFAULT
                    )
                } else if (bounds.size() > NODE_BUCKETS_MAX) {
                    log.warn(
                        "Ignoring rundeck.{}={} -- {} bands exceeds the maximum of {}. Each band multiplies series count " +
                        "across plugin_type/node_step/status. Using default '{}'.",
                        NODE_BUCKETS_PROPERTY, raw, bounds.size(), NODE_BUCKETS_MAX, NODE_BUCKETS_DEFAULT
                    )
                    bounds = null
                }
            }
            if (bounds == null) {
                bounds = parseNodeBucketBounds(NODE_BUCKETS_DEFAULT)
            }
            NodeBucketScheme scheme = buildNodeBucketScheme(bounds)
            resolvedNodeBucketScheme = scheme
            // Logged once, so an operator meeting unfamiliar node_bucket values can tell which
            // configuration produced them.
            log.info("Step node_bucket bands: {} (from bounds {})", scheme.labels, scheme.bounds)
            return scheme
        }
    }

    /**
     * Parses ascending, comma-separated upper bounds. Returns null -- never throws, and never
     * a partial list -- for anything unusable, so the caller can fall back wholesale rather
     * than silently metering against half a configuration.
     */
    private static List<Long> parseNodeBucketBounds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null
        }
        List<Long> parsed = new ArrayList<Long>()
        for (String part : raw.split(',')) {
            String trimmed = part.trim()
            if (trimmed.isEmpty()) {
                return null
            }
            long value
            try {
                value = Long.parseLong(trimmed)
            } catch (NumberFormatException ignored) {
                return null
            }
            if (value < 1L) {
                return null
            }
            // Strictly ascending: equal or descending edges would make a band unreachable.
            if (!parsed.isEmpty() && value <= parsed.get(parsed.size() - 1)) {
                return null
            }
            parsed.add(value)
        }
        parsed.isEmpty() ? null : parsed
    }

    /**
     * Derives band labels from the edges, rather than carrying hardcoded strings that could
     * drift from the numbers. For bounds [b0..bn] the first band is {@code 1} when b0 is 1 and
     * {@code 1-b0} otherwise, each subsequent band is {@code <prev+1>-<bN>}, and the open-ended
     * final band is {@code <bn>+}. So '1,10,100' yields 1 / 2-10 / 11-100 / 100+, and '1,5,10'
     * yields 1 / 2-5 / 6-10 / 10+.
     */
    private static NodeBucketScheme buildNodeBucketScheme(List<Long> bounds) {
        List<String> labels = new ArrayList<String>(bounds.size() + 1)
        for (int i = 0; i < bounds.size(); i++) {
            long upper = bounds.get(i)
            if (i == 0) {
                labels.add(upper == 1L ? '1' : ('1-' + String.valueOf(upper)))
            } else {
                labels.add(String.valueOf(bounds.get(i - 1) + 1L) + '-' + String.valueOf(upper))
            }
        }
        labels.add(String.valueOf(bounds.get(bounds.size() - 1)) + '+')
        new NodeBucketScheme(bounds, labels)
    }

    private List<Tag> jobTags(Execution execution) {
        ScheduledExecution job = execution.scheduledExecution
        if (!jobDimensionEnabled() || !job?.uuid) {
            return Collections.<Tag>emptyList()
        }
        String jobName = job.groupPath ? job.generateFullName() : job.jobName
        [Tag.of('job_id', job.uuid), Tag.of('job_name', jobName ?: 'unknown')]
    }

    private List<Tag> runningTags(Execution execution) {
        runningTagsOf(execution.project, jobTags(execution))
    }

    private List<Tag> runningTagsOf(String project, List<Tag> jobTags) {
        List<Tag> tags = [Tag.of('project', project)]
        tags.addAll(jobTags)
        tags
    }

    private AtomicInteger runningCounterFor(List<Tag> tags) {
        runningCounters.computeIfAbsent(tags, { List<Tag> key ->
            AtomicInteger counter = new AtomicInteger(0)
            Gauge.builder(
                'rundeck.executions.running',
                counter,
                { AtomicInteger c -> c.get() as double } as ToDoubleFunction<AtomicInteger>
            ).tags(key).register(meterRegistry)
            counter
        } as Function<List<Tag>, AtomicInteger>)
    }

    private Timer durationTimer(List<Tag> tags) {
        // Deliberately no publishPercentileHistogram(): that requires HdrHistogram + LatencyUtils
        // on the runtime classpath (optional micrometer-core deps, not currently in this project),
        // and would throw NoClassDefFoundError without them. This means only _count/_sum/_max are
        // exposed -- no _bucket series, so no histogram_quantile() percentiles in Grafana, only
        // average (_sum/_count) and max duration by project/status.
        Timer.builder('rundeck.execution.duration')
             .tags(tags)
             .register(meterRegistry)
    }

    private Timer stepNodeSecondsTimer(List<Tag> tags) {
        Timer.builder('rundeck.execution.step_node_duration')
             .tags(tags)
             .register(meterRegistry)
    }

    private DistributionSummary stepNodeCountSummary(List<Tag> tags) {
        DistributionSummary.builder('rundeck.execution.step_node_count')
                            .tags(tags)
                            .register(meterRegistry)
    }

    private DistributionSummary stepNodesSummary(List<Tag> tags) {
        DistributionSummary.builder('rundeck.execution.step.nodes')
                            .tags(tags)
                            .register(meterRegistry)
    }

    private Timer stepDurationTimer(List<Tag> tags) {
        Timer.builder('rundeck.execution.step.duration')
             .tags(tags)
             .register(meterRegistry)
    }

    private boolean jobDimensionEnabled() {
        configurationService != null && configurationService.getBoolean(JOB_DIMENSION_ENABLED_PROPERTY, false)
    }

    /**
     * Exposes {@code rundeck.metrics.execution.job.dimension.enabled} on the admin System
     * Configuration page, so it's discoverable/editable without hand-editing
     * rundeck-config.properties. Same pattern as ExecutionService's
     * rundeck.executionDailyMetrics.enabled entry.
     */
    @Override
    List<SysConfigProp> getSystemConfigProps() {
        [
            SystemConfig.builder().with {
                key "rundeck.metrics.execution.job.dimension.enabled"
                label "Execution Metrics: job_id/job_name dimension"
                description "Tag rundeck_executions_total/rundeck_execution_duration_seconds/" +
                    "rundeck_execution_step_node_duration/rundeck_execution_step_node_count/" +
                    "rundeck_executions_running with " +
                    "job_id and job_name (scheduled jobs only, ad-hoc executions excluded). Off by " +
                    "default: cardinality is bounded by the job catalog size, not execution volume, " +
                    "but large job catalogs should size this before enabling."
                defaultValue "false"
                required false
                restart false
                datatype "Boolean"
                visibility 'Advanced'
                category 'Execution'
                authRequired "app_admin"
                build()
            },
            SystemConfig.builder().with {
                key "rundeck.metrics.execution.step.nodeBuckets"
                label "Execution Metrics: step node_bucket bands"
                description "Ascending, comma-separated upper bounds for the node_bucket tag on " +
                    "rundeck_execution_step_nodes/rundeck_execution_step_duration. The default " +
                    "'1,10,100' produces the bands 1, 2-10, 11-100, 100+. Labels are derived from " +
                    "these numbers, so '1,5,10' gives 1, 2-5, 6-10, 10+. Takes effect on restart. " +
                    "Changing it starts new series rather than re-bucketing history, and each extra " +
                    "band multiplies series count, so keep the list short (max 20). Anything " +
                    "unparseable, non-ascending or below 1 is ignored in favour of the default."
                defaultValue NODE_BUCKETS_DEFAULT
                required false
                restart true
                datatype "String"
                visibility 'Advanced'
                category 'Execution'
                authRequired "app_admin"
                build()
            }
        ] as List<SysConfigProp>
    }

    /**
     * Removes job_id-tagged meters for a deleted job so they don't accumulate as zombie series.
     * No-op for non-DELETE job changes, or when the job dimension was never enabled (no meters to
     * find). Ad-hoc executions never carry a job_id tag, so there's nothing to clean up for them.
     */
    @Subscriber('jobChanged')
    void onJobChanged(JobChangeEvent event) {
        if (!meterRegistry || event?.eventType != JobChangeEvent.JobChangeEventType.DELETE) {
            return
        }
        String jobId = event.jobReference?.id
        if (!jobId) {
            return
        }
        [ 'rundeck.executions', 'rundeck.execution.duration', 'rundeck.execution.step_node_duration', 'rundeck.execution.step_node_count', 'rundeck.executions.running' ].each { String name ->
            List<Meter> matched = new ArrayList<Meter>(meterRegistry.find(name).tag('job_id', jobId).meters())
            matched.each { Meter meter -> meterRegistry.remove(meter.getId()) }
        }
        // deleteScheduledExecution refuses to delete a job with executions still running, so the
        // running gauge for this job_id is already at 0 here -- this only drops the now-unused
        // AtomicInteger reference from the local map, it's not correcting a nonzero count.
        runningCounters.keySet().removeIf { List<Tag> tags -> tags.any { it.key == 'job_id' && it.value == jobId } }
    }

    @Subscriber("rundeck.bootstrap")
    void initialize() {
        if (!meterRegistry) {
            log.warn("Cannot initialize execution metrics: meterRegistry is null")
            return
        }

        String version = grailsApplication?.metadata?.getProperty('info.app.version', String, null)
        String build = grailsApplication?.metadata?.getProperty('build.ident', String, null)
        String nodeUuid = frameworkService?.getServerUUID()

        Gauge.builder('rundeck.system.info', { -> 1.0d } as Supplier<Number>)
             .tag('version', version ?: 'unknown')
             .tag('build', build ?: 'unknown')
             .tag('node_uuid', nodeUuid ?: 'unknown')
             .register(meterRegistry)

        if (executionService) {
            Gauge.builder(
                'rundeck.execution.mode.active',
                { -> executionService.getExecutionsAreActive() ? 1.0d : 0.0d } as Supplier<Number>
            ).register(meterRegistry)
        }

        log.info("Registered native Micrometer execution metrics (system.info, execution.mode.active)")
    }
}
