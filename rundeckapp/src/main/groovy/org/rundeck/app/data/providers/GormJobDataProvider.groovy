package org.rundeck.app.data.providers

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.jobs.JobLifecycleComponentException
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigData
import com.dtolabs.rundeck.plugins.jobs.JobOptionImpl
import com.dtolabs.rundeck.plugins.jobs.JobPersistEventImpl
import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import grails.events.annotation.Publisher
import grails.gorm.transactions.Transactional
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j2
import org.grails.datastore.mapping.query.api.Criteria
import org.hibernate.criterion.CriteriaSpecification
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.app.data.job.schedule.DefaultJobDataChangeDetector
import org.rundeck.app.data.model.v1.DeletionResult
import org.rundeck.app.data.model.v1.job.component.JobComponentData
import org.rundeck.app.events.LogJobChangeEvent
import org.rundeck.app.job.component.JobComponentDataImportExport
import org.rundeck.app.jobs.options.JobOptionConfigPluginAttributes
import org.rundeck.core.auth.AuthConstants
import org.springframework.web.context.request.RequestContextHolder
import rundeck.Execution
import rundeck.data.job.JobReferenceImpl
import rundeck.data.job.JobRevReferenceImpl
import rundeck.data.job.RdJob
import org.rundeck.app.data.job.converters.ScheduledExecutionFromRdJobUpdater
import org.rundeck.app.data.job.converters.ScheduledExecutionToJobConverter
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import rundeck.ScheduledExecution
import rundeck.data.job.RdOption
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.JobLifecycleComponentService
import rundeck.services.JobSchedulerService
import rundeck.services.JobSchedulesService
import rundeck.services.ScheduledExecutionService
import rundeck.services.audit.JobUpdateAuditEvent
import rundeck.services.data.ScheduledExecutionDataService

import javax.persistence.EntityNotFoundException

@GrailsCompileStatic
@Transactional
@Log4j2
class GormJobDataProvider extends GormJobQueryProvider implements JobDataProvider {

    @Autowired
    ScheduledExecutionDataService scheduledExecutionDataService
    @Autowired
    RundeckJobDefinitionManager rundeckJobDefinitionManager
    @Autowired
    FrameworkService frameworkService
    @Autowired
    JobSchedulesService jobSchedulesService
    @Autowired
    JobSchedulerService jobSchedulerService
    @Autowired
    AppAuthContextProcessor rundeckAuthContextProcessor
    @Autowired
    ScheduledExecutionService scheduledExecutionService
    @Autowired
    JobLifecycleComponentService jobLifecycleComponentService

    @Override
    JobData get(Serializable id) {
        return scheduledExecutionDataService.get(id)
    }

    @Override
    JobData findByUuid(String uuid) {
        return scheduledExecutionDataService.findByUuid(uuid)
    }

    @Override
    boolean existsByUuid(String uuid) {
        return scheduledExecutionDataService.countByUuid(uuid) == 1
    }

    @Override
    JobData findByProjectAndJobNameAndGroupPath(String project, String jobName, String groupPath) {
        return ScheduledExecution.findByProjectAndJobNameAndGroupPath(project, jobName, groupPath)
    }

    @Override
    boolean existsByProjectAndJobNameAndGroupPath(String project, String jobName, String groupPath) {
        return ScheduledExecution.countByProjectAndJobNameAndGroupPath(project, jobName, groupPath) == 1
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @Override
    JobData save(JobData job) throws DataAccessException {
        def session = getSession()
        def authCtx = frameworkService.userAuthContext(session)
        ScheduledExecution se = job.uuid ? scheduledExecutionDataService.findByUuid(job.uuid) : null
        boolean isnew = false
        if(!se) {
            isnew = true
            se = new ScheduledExecution()
        }
        RdJob rdJob = (RdJob)job
        if(!rdJob.validate()) {
            throw new DataValidationException(rdJob)
        }
        LogJobChangeEvent logEvent = new LogJobChangeEvent(isnew ? 'create' : 'update','GormJobDataProvider.save', session.user)
        JobChangeData jobChangeData = detectJobChanges(se, rdJob, logEvent)
        runComponentBeforeSave(authCtx.username, rdJob)
        ScheduledExecutionFromRdJobUpdater.update(se, rdJob)
        authorizeEditAndUpdateJobUserAndRoles(authCtx, se, rdJob)
        def importedJob = validateComponents(se, rdJob)
        if(rdJob.errors.hasErrors()) throw new DataValidationException(rdJob)

        rundeckJobDefinitionManager.persistComponents(importedJob,authCtx)
        def saved = se.save(failOnError:true, flush: true)
        rundeckJobDefinitionManager.waspersisted(importedJob, authCtx)
        rescheduleJob(saved, jobChangeData)
        publishJobUpdateAuditEvent(se, isnew)
        publishLogJobChangeEvent(saved, logEvent)
        return saved
    }

    @Publisher('log.job.change.event')
    LogJobChangeEvent publishLogJobChangeEvent(ScheduledExecution se, LogJobChangeEvent event) {
        event.jobData = se
        return event
    }

    @Publisher('audit.job.update')
    JobUpdateAuditEvent publishJobUpdateAuditEvent(ScheduledExecution se, boolean isnew) {
        return new JobUpdateAuditEvent(jobUuid: se.uuid, project: se.project, fullName: se.generateFullName(), isnew: isnew)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def getSession() {
        RequestContextHolder.currentRequestAttributes().getSession()
    }

    @Override
    DeletionResult delete(Serializable id) throws DataAccessException {
        _deleteJob(ScheduledExecution.get(id))
    }

    @Override
    DeletionResult deleteByUuid(String uuid) throws DataAccessException {
        _deleteJob(ScheduledExecution.findByUuid(uuid))
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    DeletionResult _deleteJob(ScheduledExecution se) {
        if(!se) throw new EntityNotFoundException("Job not found")
        def authCtx = frameworkService.userAuthContext(getSession())
        return scheduledExecutionService.deleteScheduledExecution(se,false,authCtx,authCtx.username)
    }

    void runComponentBeforeSave(String username, RdJob rdJob) {
        INodeSet nodeSet = scheduledExecutionService.getNodes(rdJob,null)
        JobPersistEventImpl jobPersistEvent = new JobPersistEventImpl(
                rdJob.jobName,
                rdJob.project,
                convertToJobOptions(rdJob.optionSet),
                nodeSet,
                username,
                rdJob.nodeConfig.filter
        )
        def jobEventStatus
        try {
            jobEventStatus = jobLifecycleComponentService?.beforeJobSave(rdJob.project, jobPersistEvent)
        } catch (JobLifecycleComponentException exception) {
            exception.printStackTrace()
            log.debug("JobLifecycle error: " + exception.message, exception)
            rdJob.errors.reject(
                    'scheduledExecution.plugin.error.message',
                    ['Job Lifecycle: ' + exception.message].toArray(),
                    "A Plugin returned an error: " + exception.message
            )
            return
        }
        if(jobEventStatus?.isUseNewValues()) {
            rdJob.optionSet = jobEventStatus.getOptions()
                    .collect {opt -> RdOption.convertFromJobOption(opt, rdJob.optionSet?.find { it.name == opt.name })} as SortedSet<RdOption>
            rdJob.validate()
        }
    }

    ImportedJob<ScheduledExecution> validateComponents(ScheduledExecution se, RdJob rdJob) {
        def associations = [:] as Map<String, Object>
        rundeckJobDefinitionManager.jobDefinitionComponents.each{ k, val ->
            if(!(val instanceof JobComponentDataImportExport)) {
                log.warn("Job component {} cannot be imported to the job data because no importer is defined", k)
                return
            }
            JobComponentDataImportExport importer = (JobComponentDataImportExport)val
            if(!rdJob.components.containsKey(importer.componentKey)) return
            def data = importer.importFromJobData(rdJob)
            if(data) associations[k] = data
        }
        def importedJob = RundeckJobDefinitionManager.importedJob(se,associations)
        def updatedJob = rundeckJobDefinitionManager.updateJob(se, importedJob, [:])
        validateComponentsExist(rdJob)
        def rptSet = rundeckJobDefinitionManager.validateImportedJob(updatedJob)
        rptSet.validations.each { componentName, rpt ->
            rpt.errors.each { k, v  ->
                rdJob.errors.rejectValue("components",
                        "jobData.components.invalidconfiguration",
                        [componentName, k, v] as Object[],
                        'Job Component: {0} invalid config: {1} : {2}')
            }
        }
        return updatedJob
    }

    void validateComponentsExist(RdJob rdJob) {
        def jobDefinitionComponentKeys = rundeckJobDefinitionManager.jobDefinitionComponents.values().findAll{it instanceof JobComponentDataImportExport }.collect { ((JobComponentDataImportExport)it).componentKey }
        rdJob.components.each { String componentName, JobComponentData value ->
            if (!jobDefinitionComponentKeys.contains(componentName)) {
                rdJob.errors.rejectValue("components",
                        "jobData.components.notfound",
                        [componentName] as Object[],
                        'Job Component of type: {0} could not be found')
            }
        }
    }

    JobChangeData detectJobChanges(ScheduledExecution se, RdJob rdJob, LogJobChangeEvent logEvent) {
        JobChangeData jobChangeData = new JobChangeData()
        if(!se.id) return jobChangeData
        String oldjobname = se.generateJobScheduledName()
        String oldjobgroup = se.generateJobGroupName()
        jobChangeData.isScheduled = jobSchedulesService.isScheduled(se.uuid)
        DefaultJobDataChangeDetector detector = new DefaultJobDataChangeDetector(
                localScheduled: se.scheduled,
                originalCron: se.generateCrontabExression(),
                originalSchedule: se.scheduleEnabled,
                originalExecution: se.executionEnabled,
                originalTz: se.timeZone,
                originalRef: new JobRevReferenceImpl(
                        id: se.extid,
                        jobName: se.jobName,
                        groupPath: se.groupPath,
                        project: se.project,
                        version: se.version
                )
        )
        jobChangeData.renamed = detector.wasRenamed(rdJob)

        if(jobChangeData.renamed){
            logEvent.changeinfo.rename = true
            logEvent.changeinfo.origName = oldjobname
            logEvent.changeinfo.origGroup = oldjobgroup
            jobChangeData.scheduledJobName = oldjobname
            jobChangeData.scheduledGroupPath = oldjobgroup
        }

        jobChangeData.schedulingWasChanged = detector.schedulingWasChanged(rdJob)
        if (jobChangeData.schedulingWasChanged) {
            JobReferenceImpl jobReference = se.asReference() as JobReferenceImpl
            jobReference.setOriginalQuartzJobName(oldjobname)
            jobReference.setOriginalQuartzGroupName(oldjobgroup)
            jobChangeData.scheduleOwnerModified = jobSchedulerService.updateScheduleOwner(jobReference)
            jobChangeData.scheduledJobName = oldjobname
            jobChangeData.scheduledGroupPath = oldjobgroup
            if (jobChangeData.scheduleOwnerModified && frameworkService.isClusterModeEnabled()) {
                rdJob.serverNodeUUID = frameworkService.serverUUID
            }
        }
        if (!rdJob.serverNodeUUID) {
            rdJob.serverNodeUUID = frameworkService.serverUUID
        }

        jobChangeData
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void authorizeEditAndUpdateJobUserAndRoles(UserAndRolesAuthContext authContext, ScheduledExecution se, RdJob rdJob) {
        def authAction = se.id ? AuthConstants.ACTION_UPDATE : AuthConstants.ACTION_CREATE
        se.user = authContext.username
        se.userRoles = authContext.roles as List<String>
        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, se, [authAction], se.project)) {
            rdJob.errors.rejectValue('jobName', 'ScheduledExecution.jobName.unauthorized', [authAction, rdJob.jobName].toArray(), 'Unauthorized action: {0} for value: {1}')
            rdJob.errors.rejectValue('groupPath', 'ScheduledExecution.groupPath.unauthorized', [ authAction, rdJob.groupPath].toArray(), 'Unauthorized action: {0} for value: {1}')
            return
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    List<JobData> getScheduledJobsToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids, boolean ignoreInnerScheduled) {
        return ScheduledExecution.createCriteria().listDistinct {
            or {
                and {
                    if (!ignoreInnerScheduled) {
                        eq('scheduled', true)
                    }
                    if (!selectAll) {
                        if (fromServerUUID) {
                            eq('serverNodeUUID', fromServerUUID)
                        } else {
                            isNull('serverNodeUUID')
                        }
                    } else {
                        or {
                            isNull('serverNodeUUID')
                            ne('serverNodeUUID', toServerUUID)
                        }
                    }
                    if (jobids) {
                        'in'('uuid', jobids)
                    }
                }
                exists(Execution.where {
                    setAlias('exec')
                    eqProperty('jobUuid', 'this.uuid')
                    eq('status', ExecutionService.EXECUTION_SCHEDULED)
                    isNull('dateCompleted')
                    gt('dateStarted', new Date())
                    if(projectFilter){
                        eq('project',projectFilter)
                    }
                    if (!selectAll) {
                        if (fromServerUUID) {
                            eq('serverNodeUUID', fromServerUUID)
                        } else {
                            isNull('serverNodeUUID')
                        }
                    } else {
                        or {
                            isNull('serverNodeUUID')
                            ne('serverNodeUUID', toServerUUID)
                        }
                    }
                    projections {
                        property('id')
                    }
                })
            }

            if (projectFilter) {
                eq('project', projectFilter)
            }
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    List<JobData> getJobsWithAdhocScheduledExecutionsToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter) {
        return ScheduledExecution.createCriteria().listDistinct {
            applyAdhocScheduledExecutionsCriteria(delegate, selectAll, fromServerUUID, toServerUUID, projectFilter)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void applyAdhocScheduledExecutionsCriteria(Criteria delegate, boolean selectAll, String fromServerUUID, String toServerUUID, String project){
        delegate.executions(CriteriaSpecification.LEFT_JOIN) {
            eq('status', ExecutionService.EXECUTION_SCHEDULED)
            isNull('dateCompleted')
            gt('dateStarted', new Date())
            if(project){
                eq('project',project)
            }
            if (!selectAll) {
                if (fromServerUUID) {
                    eq('serverNodeUUID', fromServerUUID)
                } else {
                    isNull('serverNodeUUID')
                }
            } else {
                or {
                    isNull('serverNodeUUID')
                    ne('serverNodeUUID', toServerUUID)
                }
            }
        }
    }

    void rescheduleJob(ScheduledExecution se, JobChangeData jobChangeData) {
        scheduledExecutionService.rescheduleJob(se, jobChangeData.isScheduled,
        jobChangeData.scheduledJobName, jobChangeData.scheduledGroupPath, false,
        !jobChangeData.schedulingWasChanged || !jobChangeData.scheduleOwnerModified)
    }

    SortedSet<JobOption> convertToJobOptions(SortedSet<RdOption> rdOptions) {
        def opts = new TreeSet<JobOption>()
        if(!rdOptions) return opts

        opts.addAll(rdOptions.collect {opt ->
            JobOptionConfigData jobOptionConfigData= new JobOptionConfigData()
            jobOptionConfigData.addConfig(new JobOptionConfigPluginAttributes(opt.configMap))

            JobOptionImpl.builder()
            .name(opt.name)
            .description(opt.description)
            .defaultValue(opt.defaultValue)
            .delimiter(opt.delimiter)
            .defaultStoragePath(opt.defaultStoragePath)
            .isDate(opt.isDate)
            .dateFormat(opt.dateFormat)
            .regex(opt.regex)
            .enforced(opt.enforced)
            .hidden(opt.hidden)
            .optionType(opt.optionType)
            .label(opt.label)
            .required(opt.required)
            .realValuesUrl(opt.realValuesUrl)
            .sortIndex(opt.sortIndex)
            .optionValues(opt.optionValues)
            .optionValuesPluginType(opt.optionValuesPluginType)
            .secureExposed(opt.secureExposed)
            .secureInput(opt.secureInput)
            .configData(jobOptionConfigData)
            .multivalueAllSelected(opt.multivalueAllSelected)
            .multivalued(opt.multivalued)
            .sortValues(opt.sortValues)
            .valuesListDelimiter(opt.valuesListDelimiter)
            .valuesList(opt.valuesList)
            .build()
        })
        opts
    }

    static ObjectMapper mapper = new ObjectMapper()

    static class JobChangeData {
        String scheduledJobName
        String scheduledGroupPath
        boolean isScheduled
        boolean renamed
        boolean scheduleOwnerModified
        boolean schedulingWasChanged
    }
}
