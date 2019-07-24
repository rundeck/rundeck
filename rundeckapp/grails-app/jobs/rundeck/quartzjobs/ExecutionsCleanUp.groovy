package rundeck.quartzjobs

import com.dtolabs.rundeck.app.support.ExecutionQuery
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnableToInterruptJobException
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.services.*
import rundeck.services.jobs.ResolvedAuthJobService

class ExecutionsCleanUp implements InterruptableJob {
    static Logger logger = Logger.getLogger(ExecutionsCleanUp)
    def boolean wasInterrupted

    void interrupt() throws UnableToInterruptJobException {
        wasInterrupted = true
    }


    void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Initializing cleaner execution history job")

        String project = context.jobDetail.jobDataMap.get('project')
        String maxDaysToKeep = context.jobDetail.jobDataMap.get('maxDaysToKeep')
        String minimumExecutionToKeep = context.jobDetail.jobDataMap.get('minimumExecutionToKeep')
        String maximumDeletionSize = context.jobDetail.jobDataMap.get('maximumDeletionSize')

        logger.info("Cleaner parameters: Project name: ${project}")
        logger.info("Max days to keep: ${maxDaysToKeep}")
        logger.info("Minimum executions to keep: ${minimumExecutionToKeep}")
        logger.info("Maximum size of deletions: ${maximumDeletionSize ?: '500 (default)'}")

        FrameworkService frameworkService = fetchFrameworkService(context.jobDetail.jobDataMap)
        ExecutionService executionService = fetchExecutionService(context.jobDetail.jobDataMap)
        FileUploadService fileUploadService = fetchFileUploadService(context.jobDetail.jobDataMap)
        LogFileStorageService logFileStorageService = fetchLogFileStorageService(context.jobDetail.jobDataMap)
        JobSchedulerService jobSchedulerService = fetchJobSchedulerService(context.jobDetail.jobDataMap)

        if(!wasInterrupted) {
            List execIdsToExclude = searchExecutions(frameworkService, executionService, jobSchedulerService, project,
                    maxDaysToKeep ? Integer.parseInt(maxDaysToKeep) : 0,
                    minimumExecutionToKeep ? Integer.parseInt(minimumExecutionToKeep) : 0,
                    maximumDeletionSize ? Integer.parseInt(maximumDeletionSize) : 500)
            logger.info("Executions to delete: ${execIdsToExclude.toListString()}")
            deleteByExecutionList(execIdsToExclude,fileUploadService, logFileStorageService)
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
            ExecReport.findAllByJcExecId(e.id.toString()).each { rpt ->
                rpt.delete()
            }

            List<File> files = []
            def execs = []
            //aggregate all files to delete
            execs << e
            [LoggingService.LOG_FILE_FILETYPE, WorkflowService.STATE_FILE_FILETYPE].each { ftype ->
                def file = logFileStorageService.getFileForExecutionFiletype(e, ftype, true, false)
                if (null != file && file.exists()) {
                    files << file
                }
                def fileb = logFileStorageService.getFileForExecutionFiletype(e, ftype, true, true)
                if (null != fileb && fileb.exists()) {
                    files << fileb
                }
                def file2 = logFileStorageService.getFileForExecutionFiletype(e, ftype, false, false)
                if (null != file2 && file2.exists()) {
                    files << file2
                }
                def file2b = logFileStorageService.getFileForExecutionFiletype(e, ftype, false, true)
                if (null != file2b && file2b.exists()) {
                    files << file2b
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
            e.delete()
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
            logger.error("Failed to delete execution ${e.id}", ex)
            result = [error:'failure',message: "Failed to delete execution {{Execution ${e.id}}}: ${ex.message}", success: false]
        }
        return result
    }

    private List<Long> searchExecutions(FrameworkService frameworkService, ExecutionService executionService, JobSchedulerService jobSchedulerService, String project, Integer maxDaysToKeep,
                                             Integer minimumExecutionToKeep, Integer maximumDeletionSize = 500){
        List collectedExecutions= []
        List<String> listDeadMembers = null
        def serverUUID = frameworkService.getServerUUID()
        def removeNullServerUUID = false

        if(frameworkService.isClusterModeEnabled()){
            listDeadMembers = jobSchedulerService.getDeadMembers(serverUUID);
            logger.info("searching executions of node ID: ${serverUUID}")

            if(listDeadMembers){
                logger.info("list dead nodes to size: ${listDeadMembers.size()}")
                logger.info("list dead nodes to check: ${listDeadMembers.toString()}")

                if(listDeadMembers.contains("null")){
                    removeNullServerUUID=true
                    listDeadMembers.remove("null")
                }
            }
        }

        Map jobList = executionService.queryExecutions(createCriteria(
                project,
                maxDaysToKeep,
                maximumDeletionSize,
                frameworkService.isClusterModeEnabled()?serverUUID:null,
                frameworkService.isClusterModeEnabled()?listDeadMembers:null,
                removeNullServerUUID)

        )

        if(null != jobList && null != jobList.get("total")) {
            Integer totalToExclude = (Integer) jobList.get("total")

            logger.info("found ${totalToExclude} executions")
            if(totalToExclude >0) {
                List result = (List)jobList.get("result")
                //result.sort{a,b -> b.dateCompletedgreg  <=> a.dateCompleted}

                if (minimumExecutionToKeep > 0) {
                    int totalExecutions = this.totalAllExecutions(executionService, project)
                    int sub = totalExecutions - totalToExclude
                    logger.info("minimum executions to keep: ${minimumExecutionToKeep}")
                    logger.info("total exections of project ${project}: ${totalExecutions}")
                    logger.info("total to exclude: ${totalToExclude}")
                    if (sub < minimumExecutionToKeep) {
                        int jump = minimumExecutionToKeep - sub
                        logger.info("${jump} executions can not be removed")
                        result = jump < result.size() ? result[jump..result.size() - 1] : []
                        logger.info("${result.size()} executions will be removed")
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

    private Closure createCriteria(String project, Integer maxDaysToKeep = 0, Integer maxDetetionSize = 500, String serverNodeUUID = null, List<String> deadMembers = null, boolean removeNullServerUUID = false){
        Date endDate=ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d")
        return {isCount ->
            if(!isCount){
                projections {
                    //just return the ID on the select
                    property('id')
                }
            }

            if(serverNodeUUID){
                if(!deadMembers){
                    eq('serverNodeUUID', serverNodeUUID)
                }else{
                    deadMembers.add(serverNodeUUID)
                    if(removeNullServerUUID){
                        //if deadMembers contain null, it will also remove the serverNodeUUID=null
                        or {
                            isNull('serverNodeUUID')
                            'in'('serverNodeUUID', deadMembers)
                        }
                    }else{
                        'in'('serverNodeUUID', deadMembers)
                    }

                }
            } else {
                isNull('serverNodeUUID')
            }

            eq('project', project)
            le('dateCompleted', endDate)
            //remove running execution
            isNotNull('dateCompleted')
            ne('status',ExecutionService.EXECUTION_SCHEDULED)
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
