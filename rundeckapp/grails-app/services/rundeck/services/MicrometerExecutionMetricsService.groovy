package rundeck.services

import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.core.GrailsApplication
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
                    "rundeck_executions_running with job_id and job_name (scheduled jobs only, " +
                    "ad-hoc executions excluded). Off by default: cardinality is bounded by the " +
                    "job catalog size, not execution volume, but large job catalogs should size " +
                    "this before enabling."
                defaultValue "false"
                required false
                restart false
                datatype "Boolean"
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
        [ 'rundeck.executions', 'rundeck.execution.duration', 'rundeck.executions.running' ].each { String name ->
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
