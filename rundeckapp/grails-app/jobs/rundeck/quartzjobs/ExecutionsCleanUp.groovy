package rundeck.quartzjobs

import com.dtolabs.rundeck.app.support.ExecutionQuery
import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnableToInterruptJobException
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.services.*
import rundeck.services.jobs.ResolvedAuthJobService

class ExecutionsCleanUp implements InterruptableJob {
    static Logger logger = LoggerFactory.getLogger(ExecutionsCleanUp)
    boolean wasInterrupted

    void interrupt() throws UnableToInterruptJobException {
        wasInterrupted = true
    }


    void execute(JobExecutionContext context) throws JobExecutionException {
        JobSchedulerService jobSchedulerService = fetchJobSchedulerService(context.jobDetail.jobDataMap)
        FrameworkService frameworkService = fetchFrameworkService(context.jobDetail.jobDataMap)
        ReportService reportService = fetchReportService(context.jobDetail.jobDataMap)
        String project = context.jobDetail.jobDataMap.get('project')
        String uuid = frameworkService.getServerUUID()

        logger.info("Initializing cleaner execution history job from server ${uuid}")

        String maxDaysToKeep = context.jobDetail.jobDataMap.get('maxDaysToKeep')
        String minimumExecutionToKeep = context.jobDetail.jobDataMap.get('minimumExecutionToKeep')
        String maximumDeletionSize = context.jobDetail.jobDataMap.get('maximumDeletionSize')
        String executionsToKeepPerJob = context.jobDetail.jobDataMap.get('executionsToKeepPerJob')

        logger.info("Cleaner parameters: Project name: ${project}")
        logger.info("Max days to keep: ${maxDaysToKeep}")
        logger.info("Minimum executions to keep: ${minimumExecutionToKeep}")
        logger.info("Maximum size of deletions: ${maximumDeletionSize ?: '500 (default)'}")
        logger.info("Executions to keep per job: ${executionsToKeepPerJob ?: '0 (disabled)'}")

        ExecutionService executionService = fetchExecutionService(context.jobDetail.jobDataMap)
        FileUploadService fileUploadService = fetchFileUploadService(context.jobDetail.jobDataMap)
        LogFileStorageService logFileStorageService = fetchLogFileStorageService(context.jobDetail.jobDataMap)
        ReferencedExecutionDataProvider referencedExecutionDataProvider = fetchReferencedExecutionDataProvider(context.jobDetail.jobDataMap)

        if(!wasInterrupted) {
            List execIdsToExclude = searchExecutions(frameworkService, executionService, jobSchedulerService, project,
                    maxDaysToKeep ? Integer.parseInt(maxDaysToKeep) : 0,
                    minimumExecutionToKeep ? Integer.parseInt(minimumExecutionToKeep) : 0,
                    maximumDeletionSize ? Integer.parseInt(maximumDeletionSize) : 500,
                    executionsToKeepPerJob ? Integer.parseInt(executionsToKeepPerJob) : 0)
            logger.info("Executions to delete: ${execIdsToExclude.toListString()}")
            deleteByExecutionList(execIdsToExclude, fileUploadService, logFileStorageService, referencedExecutionDataProvider, reportService)
        }
    }

    private Map deleteBulkExecutionIds(List<Long> execs, FileUploadService fileUploadService,
                                LogFileStorageService logFileStorageService, ReferencedExecutionDataProvider referencedExecutionDataProvider, ReportService reportService) {
        def failures=[]
        def failed=false
        def count=0
        for (Long exec : execs) {
            def result
            if (!exec) {
                result = [success: false, message: 'Execution Not found: ' + exec, id: exec]
            } else {
                result = deleteExecution(exec, fileUploadService, logFileStorageService, referencedExecutionDataProvider, reportService)
                result.id = exec
            }
            if(!result.success){
                failed=true
                failures<<result
            }else{
                count++
            }
        }

        return [success:!failed, failures:failures, successTotal:count]
    }

    private Map deleteExecution(Long execId, FileUploadService fileUploadService, LogFileStorageService logFileStorageService, ReferencedExecutionDataProvider referencedExecutionDataProvider, ReportService reportService){
        Map result
        try {
            Execution e = Execution.findById(execId)

            if (e.dateCompleted == null && e.dateStarted != null) {
                return [error: 'running', message: "Failed to delete execution {{Execution ${e.id}}}: The execution is currently running", success: false]
            }

            referencedExecutionDataProvider.deleteByExecutionId(e.id)

            //delete all reports
            reportService.deleteByExecution(e)
            def executionFiles = logFileStorageService.getExecutionFiles(e, [], false)

            List<File> files = []
            def execs = []
            //aggregate all files to delete
            execs << e
            executionFiles.each { ftype, executionFile ->

                def localFile = logFileStorageService.getFileForExecutionFiletype(e, ftype, false, false)
                if (null != localFile && localFile.exists()) {
                    files << localFile
                }

                def partialFile = logFileStorageService.getFileForExecutionFiletype(e, ftype, false, true)
                if (null != partialFile && partialFile.exists()) {
                    files << partialFile
                }

                def resultDeleteRemote = logFileStorageService.removeRemoteLogFile(e, ftype)
                if(!resultDeleteRemote.started){
                    logger.debug(resultDeleteRemote.error)
                }
            }
            //delete all job file records
            fileUploadService.deleteRecordsForExecution(e)

            logger.debug("${files.size()} files from execution will be deleted")
            //delete execution
            //find an execution that this is a retry for
            Execution.findAllByRetryExecution(e).each{e2->
                e2.retryExecution=null
            }
            LogFileStorageRequest.findByExecution(e)?.delete(flush: true)
            e.delete(flush: true)
            //delete all files
            def deletedfiles = 0
            files.each { file ->
                if (!FileUtils.deleteQuietly(file)) {
                    logger.warn("Failed to delete file while deleting execution ${e.id}: ${file.absolutePath}")
                } else {
                    deletedfiles++
                }
            }
            logger.debug("${deletedfiles} files removed")
            logger.info("Deleted execution: ${e.id}")
            result = [success: true]
        } catch (Exception ex) {
            logger.error("Failed to delete execution ${execId}", ex)
            result = [error:'failure',message: "Failed to delete execution {{Execution ${execId}}}: ${ex.message}", success: false]
        }
        return result
    }

    /**
     * Find the ids of the executions to delete in a project.
     *
     * @param project project name
     * @param maxDaysToKeep executions completed more than this many days ago are candidates for deletion
     * @param minimumExecutionToKeep minimum number of executions to keep in the whole project, 0 to disable
     * @param maximumDeletionSize maximum number of execution ids to return
     * @param executionsToKeepPerJob number of most recent completed executions to keep for each job, 0 to disable.
     *        See {@link #findCandidateExecutionsKeepingPerJob}
     * @return execution ids to delete
     */
    private List<Long> searchExecutions(FrameworkService frameworkService, ExecutionService executionService, JobSchedulerService jobSchedulerService, String project, Integer maxDaysToKeep,
                                             Integer minimumExecutionToKeep, Integer maximumDeletionSize = 500,
                                             Integer executionsToKeepPerJob = 0){
        List collectedExecutions= []

        logger.info("Searching All Executions")

        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", null)

        List<Long> jobList = findCandidateExecutions(project, endDate, maximumDeletionSize, executionsToKeepPerJob ?: 0)

        if(null != jobList && 0 != jobList.size()) {
            List result = jobList
            Integer totalFound = jobList.size()
            Integer totalToExclude = result.size()

            logger.info("found ${totalFound} executions")
            if(totalToExclude >0) {
                if (minimumExecutionToKeep > 0) {

                    int totalExecutions = this.totalAllExecutions(project)
                    int sub = totalExecutions - totalToExclude

                    logger.info("minimum executions to keep: ${minimumExecutionToKeep}")
                    logger.info("total exections of project ${project}: ${totalExecutions}")
                    logger.info("total to exclude: ${totalToExclude}")

                    boolean removeSubResult= false
                    boolean remove= true

                    if(totalExecutions < minimumExecutionToKeep){
                        remove = false
                    }

                    if (sub < minimumExecutionToKeep) {
                        removeSubResult= true
                    }

                    if (removeSubResult) {
                        int jump = minimumExecutionToKeep - sub
                        logger.info("${jump} executions can not be removed")
                        //remove the oldest executions
                        def finalIndex = result.size() - jump
                        result = jump < result.size() ? result.subList(0, finalIndex) : []
                        logger.info("${result.size()} executions will be removed")
                    }

                    if(!remove){
                        logger.info("${result.size()} executions can not be removed ")
                        result = []
                    }
                }

                collectedExecutions.addAll(result)
            }
        }


        if(collectedExecutions.size()==0){
            logger.info("No executions to delete")
        }
        return collectedExecutions
    }

    /**
     * Find the ids of executions completed on or before the end date, up to the maximum deletion size.
     *
     * @param project project name
     * @param endDate executions completed on or before this date are candidates
     * @param maximumDeletionSize maximum number of ids to return
     * @param executionsToKeepPerJob number of most recent completed executions to keep for each job, 0 to disable
     * @return candidate execution ids
     */
    @CompileStatic
    private List<Long> findCandidateExecutions(
        String project,
        Date endDate,
        int maximumDeletionSize,
        int executionsToKeepPerJob
    ) {
        if (executionsToKeepPerJob > 0) {
            return findCandidateExecutionsKeepingPerJob(project, endDate, maximumDeletionSize, executionsToKeepPerJob)
        }
        return (List<Long>) Execution.executeQuery(
            """select e.id from Execution e 
               where e.project = :project 
               and e.dateCompleted <= :endDate""",
            [
                project: project,
                endDate: endDate
            ],
            [max: maximumDeletionSize]
        )
    }

    /**
     * Find candidate execution ids while keeping the most recent completed executions of each job.
     * <p>
     * Executions are grouped by {@code jobUuid}, which is kept when a job is deleted, so the executions of a
     * deleted job are protected too. Executions without a {@code jobUuid} (ad hoc commands) have no job, so only
     * the project-wide minimum applies to them.
     * </p>
     * <p>
     * "Most recent" means ordered by {@code dateCompleted}, then by {@code id} to break ties. Running executions
     * are not counted. The protected executions are excluded in the queries, not filtered after a limited fetch,
     * so a batch full of protected executions cannot stop the cleanup. Every query filters on the job uuid and
     * project, so it can use the {@code (job_uuid, project, date_completed)} index.
     * </p>
     *
     * @param project project name
     * @param endDate executions completed on or before this date are candidates
     * @param maximumDeletionSize maximum number of ids to return
     * @param executionsToKeepPerJob number of most recent completed executions to keep for each job, must be &gt; 0
     * @return candidate execution ids
     */
    @CompileStatic
    private List<Long> findCandidateExecutionsKeepingPerJob(
        String project,
        Date endDate,
        int maximumDeletionSize,
        int executionsToKeepPerJob
    ) {
        List<Long> candidates = []
        candidates.addAll((List<Long>) Execution.executeQuery(
            """select e.id from Execution e
               where e.project = :project
               and e.dateCompleted <= :endDate
               and e.jobUuid is null""",
            [project: project, endDate: endDate],
            [max: maximumDeletionSize]
        ))
        if (candidates.size() >= maximumDeletionSize) {
            return candidates
        }
        List<String> jobUuids = (List<String>) Execution.executeQuery(
            """select distinct e.jobUuid from Execution e
               where e.project = :project
               and e.dateCompleted <= :endDate
               and e.jobUuid is not null""",
            [project: project, endDate: endDate]
        )
        logger.info("found ${jobUuids.size()} jobs with executions older than the retention period")
        for (String jobUuid : jobUuids) {
            int remaining = maximumDeletionSize - candidates.size()
            if (remaining <= 0 || wasInterrupted) {
                break
            }
            candidates.addAll(findJobCandidateExecutions(project, jobUuid, endDate, executionsToKeepPerJob, remaining))
        }
        return candidates
    }

    /**
     * Find candidate execution ids of one job, excluding its most recent completed executions.
     *
     * @param project project name
     * @param jobUuid job uuid
     * @param endDate executions completed on or before this date are candidates
     * @param executionsToKeep number of most recent completed executions to keep, must be &gt; 0
     * @param max maximum number of ids to return
     * @return candidate execution ids, empty if the job has no more than {@code executionsToKeep} completed executions
     */
    @CompileStatic
    private List<Long> findJobCandidateExecutions(
        String project,
        String jobUuid,
        Date endDate,
        int executionsToKeep,
        int max
    ) {
        //the oldest execution that is kept: the Nth most recent completed execution of the job
        List cutoff = Execution.executeQuery(
            """select e.dateCompleted, e.id from Execution e
               where e.jobUuid = :jobUuid
               and e.project = :project
               and e.dateCompleted is not null
               order by e.dateCompleted desc, e.id desc""",
            [jobUuid: jobUuid, project: project],
            [offset: executionsToKeep - 1, max: 1]
        )
        if (!cutoff) {
            return []
        }
        Object[] row = (Object[]) cutoff[0]
        return (List<Long>) Execution.executeQuery(
            """select e.id from Execution e
               where e.jobUuid = :jobUuid
               and e.project = :project
               and e.dateCompleted <= :endDate
               and (e.dateCompleted < :cutoffDate
                    or (e.dateCompleted = :cutoffDate and e.id < :cutoffId))""",
            [jobUuid: jobUuid, project: project, endDate: endDate, cutoffDate: (Date) row[0], cutoffId: (Long) row[1]],
            [max: max]
        )
    }

    private int totalAllExecutions(String project){
        Integer total = Execution.executeQuery(
                "select count(e.id) from Execution e where e.project = :project",
                [project: project]
        )[0]
        return null != total ? total : 0
    }


    private int deleteByExecutionList(List<Long> collectedExecutions, FileUploadService fileUploadService, LogFileStorageService logFileStorageService,referencedExecutionDataProvider, ReportService reportService) {
        logger.info("Start to delete ${collectedExecutions.size()} executions")
        if(collectedExecutions.size()>0) {
            Map result = deleteBulkExecutionIds(collectedExecutions, fileUploadService, logFileStorageService, referencedExecutionDataProvider, reportService)
            if (result != null) {
                List failureList = new ArrayList<>();
                List<Map> resultList = (List<Map>) result.get("failures")
                if(resultList != null) {
                    for (Map res : resultList) {
                        if (!(Boolean) result.get("success")) {
                            failureList.add((String) res.get("message"))
                            logger.info(res.get("message"))
                        }
                    }
                }
                Integer successTotal = (Integer) result.get("successTotal")
                logger.info("Deleted ${successTotal} of ${collectedExecutions.size()} executions")
                if(successTotal<collectedExecutions.size()){
                    logger.error("Some executions weren't deleted")
                }
                return successTotal
            }
        }
        return 0
    }

    private ExecutionService fetchExecutionService(def jobDataMap) {
        def es = jobDataMap.get("executionService")
        if (es==null) {
            throw new RuntimeException("ExecutionService could not be retrieved from JobDataMap!")
        }
        if (! (es instanceof ExecutionService)) {
            throw new RuntimeException("JobDataMap contained invalid ExecutionService type: " + es.getClass().getName())
        }
        return es

    }

    private ResolvedAuthJobService fetchResolvedAuthJobService(def jobDataMap) {
        def raj = jobDataMap.get("resolvedAuthJobService")
        if (raj==null) {
            throw new RuntimeException("ResolvedAuthJobService could not be retrieved from JobDataMap!")
        }
        if (! (raj instanceof ResolvedAuthJobService)) {
            throw new RuntimeException("JobDataMap contained invalid ResolvedAuthJobService type: " + raj.getClass().getName())
        }
        return raj

    }

    private FileUploadService fetchFileUploadService(def jobDataMap) {
        def fu = jobDataMap.get("fileUploadService")
        if (fu==null) {
            throw new RuntimeException("FileUploadService could not be retrieved from JobDataMap!")
        }
        if (! (fu instanceof FileUploadService)) {
            throw new RuntimeException("JobDataMap contained invalid FileUploadService type: " + fu.getClass().getName())
        }
        return fu

    }

    private LogFileStorageService fetchLogFileStorageService(def jobDataMap) {
        def lfs = jobDataMap.get("logFileStorageService")
        if (lfs==null) {
            throw new RuntimeException("logFileStorageService could not be retrieved from JobDataMap!")
        }
        if (! (lfs instanceof LogFileStorageService)) {
            throw new RuntimeException("JobDataMap contained invalid logFileStorageService type: " + lfs.getClass().getName())
        }
        return lfs

    }

    private FrameworkService fetchFrameworkService(def jobDataMap) {
        def fws = jobDataMap.get("frameworkService")
        if (fws==null) {
            throw new RuntimeException("frameworkService could not be retrieved from JobDataMap!")
        }
        if (! (fws instanceof FrameworkService)) {
            throw new RuntimeException("JobDataMap contained invalid frameworkService type: " + fws.getClass().getName())
        }
        return fws

    }


    private JobSchedulerService fetchJobSchedulerService(def jobDataMap){
        def jobSchedulerService = jobDataMap.get("jobSchedulerService")
        if (jobSchedulerService==null) {
            throw new RuntimeException("jobSchedulerService could not be retrieved from JobDataMap!")
        }
        if (! (jobSchedulerService instanceof JobSchedulerService)) {
            throw new RuntimeException("JobDataMap contained invalid JobSchedulerService type: " + jobSchedulerService.getClass().getName())
        }
        return jobSchedulerService
    }


    private ReportService fetchReportService(def jobDataMap) {
        def fws = jobDataMap.get("reportService")
        if (fws==null) {
            throw new RuntimeException("reportService could not be retrieved from JobDataMap!")
        }
        if (! (fws instanceof ReportService)) {
            throw new RuntimeException("JobDataMap contained invalid reportService type: " + fws.getClass().getName())
        }
        return fws
    }

    private ReferencedExecutionDataProvider fetchReferencedExecutionDataProvider(def jobDataMap){
        def referencedExecutionDataProvider = jobDataMap.get("referencedExecutionDataProvider")
        if (referencedExecutionDataProvider==null) {
            throw new RuntimeException("referencedExecutionDataProvider could not be retrieved from JobDataMap!")
        }
        if (! (referencedExecutionDataProvider instanceof ReferencedExecutionDataProvider)) {
            throw new RuntimeException("JobDataMap contained invalid ReferencedExecutionDataProvider type: " + referencedExecutionDataProvider.getClass().getName())
        }
        return referencedExecutionDataProvider
    }
}
