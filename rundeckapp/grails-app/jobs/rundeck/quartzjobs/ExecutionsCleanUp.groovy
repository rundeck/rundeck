package rundeck.quartzjobs

import com.dtolabs.rundeck.app.support.ExecutionQuery
import org.apache.commons.io.FileUtils
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnableToInterruptJobException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ReferencedExecution
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
        String project = context.jobDetail.jobDataMap.get('project')
        String uuid = frameworkService.getServerUUID()

        logger.info("Initializing cleaner execution history job from server ${uuid}")

        String maxDaysToKeep = context.jobDetail.jobDataMap.get('maxDaysToKeep')
        String minimumExecutionToKeep = context.jobDetail.jobDataMap.get('minimumExecutionToKeep')
        String maximumDeletionSize = context.jobDetail.jobDataMap.get('maximumDeletionSize')

        logger.info("Cleaner parameters: Project name: ${project}")
        logger.info("Max days to keep: ${maxDaysToKeep}")
        logger.info("Minimum executions to keep: ${minimumExecutionToKeep}")
        logger.info("Maximum size of deletions: ${maximumDeletionSize ?: '500 (default)'}")

        ExecutionService executionService = fetchExecutionService(context.jobDetail.jobDataMap)
        FileUploadService fileUploadService = fetchFileUploadService(context.jobDetail.jobDataMap)
        LogFileStorageService logFileStorageService = fetchLogFileStorageService(context.jobDetail.jobDataMap)

        if(!wasInterrupted) {
            List execIdsToExclude = searchExecutions(frameworkService, executionService, jobSchedulerService, project,
                    maxDaysToKeep ? Integer.parseInt(maxDaysToKeep) : 0,
                    minimumExecutionToKeep ? Integer.parseInt(minimumExecutionToKeep) : 0,
                    maximumDeletionSize ? Integer.parseInt(maximumDeletionSize) : 500)
            logger.info("Executions to delete: ${execIdsToExclude.toListString()}")
            deleteByExecutionList(execIdsToExclude, fileUploadService, logFileStorageService)
        }
    }

    private Map deleteBulkExecutionIds(List<Long> execs, FileUploadService fileUploadService,
                                LogFileStorageService logFileStorageService) {
        def failures=[]
        def failed=false
        def count=0
        for (Long exec : execs) {
            def result
            if (!exec) {
                result = [success: false, message: 'Execution Not found: ' + exec, id: exec]
            } else {
                result = deleteExecution(exec, fileUploadService, logFileStorageService)
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

    private Map deleteExecution(Long execId, FileUploadService fileUploadService, LogFileStorageService logFileStorageService){
        Map result
        try {
            Execution e = Execution.findById(execId)

            if (e.dateCompleted == null && e.dateStarted != null) {
                return [error: 'running', message: "Failed to delete execution {{Execution ${e.id}}}: The execution is currently running", success: false]
            }

            ReferencedExecution.findAllByExecution(e).each{ re ->
                re.delete()
            }
            //delete all reports
            ExecReport.findAllByExecutionId(e.id).each { rpt ->
                rpt.delete()
            }

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

    private List<Long> searchExecutions(FrameworkService frameworkService, ExecutionService executionService, JobSchedulerService jobSchedulerService, String project, Integer maxDaysToKeep,
                                             Integer minimumExecutionToKeep, Integer maximumDeletionSize = 500){
        List collectedExecutions= []

        logger.info("Searching All Executions")

        Map jobList = executionService.queryExecutions(
            getExecutionsQueryCriteria(
                project,
                maxDaysToKeep,
                maximumDeletionSize
            )
        )

        if(null != jobList && null != jobList.get("total")) {
            List result = (List)jobList.get("result")
            Integer totalFound = (Integer) jobList.get("total")
            Integer totalToExclude = result.size()

            logger.info("found ${totalFound} executions")
            if(totalToExclude >0) {
                if (minimumExecutionToKeep > 0) {

                    int totalExecutions = this.totalAllExecutions(executionService, project)
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

    private int totalAllExecutions(ExecutionService executionService, String project){
        ExecutionQuery query = new ExecutionQuery(projFilter: project)
        def total = Execution.createCriteria().count{
            def queryCriteria = query.createCriteria(delegate)
            queryCriteria()
        }
        return null != total ? total : 0
    }

    private Closure getExecutionsQueryCriteria(String project, Integer maxDaysToKeep = 0, Integer maxDetetionSize = 500){
        Date endDate=ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d")
        return {isCount ->
            if(!isCount){
                projections {
                    //just return the ID on the select
                    property('id')
                }
            }

            eq('project', project)
            le('dateCompleted', endDate)
            //remove running execution
            isNotNull('dateCompleted')
            and{
                ne('status',ExecutionService.EXECUTION_SCHEDULED)
                ne('status', ExecutionService.EXECUTION_QUEUED)
            }
            maxResults(maxDetetionSize)
            if (!isCount) {
                and {
                    order('dateCompleted', 'asc')
                }
            }
        }
    }

    private int deleteByExecutionList(List<Long> collectedExecutions, FileUploadService fileUploadService, LogFileStorageService logFileStorageService) {
        logger.info("Start to delete ${collectedExecutions.size()} executions")
        if(collectedExecutions.size()>0) {
            Map result = deleteBulkExecutionIds(collectedExecutions, fileUploadService, logFileStorageService)
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
}
