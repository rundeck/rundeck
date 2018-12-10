package rundeck.quartzjobs

import com.dtolabs.rundeck.app.support.ExecutionQuery
import org.apache.commons.io.FileUtils
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnableToInterruptJobException
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.services.*
import rundeck.services.jobs.ResolvedAuthJobService

class ExecutionsCleanerJob  implements InterruptableJob {
    def boolean wasInterrupted

    void interrupt() throws UnableToInterruptJobException {
        wasInterrupted = true
    }


    void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Initializing cleaner execution history job")

        String project = context.jobDetail.jobDataMap.get('project')
        String maxDaysToKeep = context.jobDetail.jobDataMap.get('maxDaysToKeep')
        String minimumExecutionToKeep = context.jobDetail.jobDataMap.get('minimumExecutionToKeep')
        String maximumDeletionSize = context.jobDetail.jobDataMap.get('maximumDeletionSize')

        log.info("Cleaner parameters: Project name: ${project}")
        log.info("Max days to keep: ${maxDaysToKeep}")
        log.info("Minimum executions to keep: ${minimumExecutionToKeep}")
        log.info("Maximum size of deletions: ${maximumDeletionSize ?: '500 (default)'}")

        FrameworkService frameworkService = fetchFrameworkService(context.jobDetail.jobDataMap)
        ExecutionService executionService = fetchExecutionService(context.jobDetail.jobDataMap)
        FileUploadService fileUploadService = fetchFileUploadService(context.jobDetail.jobDataMap)
        LogFileStorageService logFileStorageService = fetchLogFileStorageService(context.jobDetail.jobDataMap)

        if(!wasInterrupted) {
            List execIdsToExclude = searchExecutions(frameworkService, executionService, project,
                    maxDaysToKeep ? Integer.parseInt(maxDaysToKeep) : 0,
                    minimumExecutionToKeep ? Integer.parseInt(minimumExecutionToKeep) : 0,
                    maximumDeletionSize ? Integer.parseInt(maximumDeletionSize) : 500)
            log.info("Executions to delete: ${execIdsToExclude.toListString()}")
            deleteByExecutionList(execIdsToExclude,fileUploadService, logFileStorageService)
        }
    }

    private Map deleteBulkExecutionIds(List<Execution> execs, FileUploadService fileUploadService,
                                LogFileStorageService logFileStorageService) {
        def failures=[]
        def failed=false
        def count=0
        for (Execution exec : execs) {
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

    private Map deleteExecution(Execution e, FileUploadService fileUploadService, LogFileStorageService logFileStorageService){
        Map result
        try {
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

            log.debug("${files.size()} files from execution will be deleted")
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
                    log.warn("Failed to delete file while deleting execution ${e.id}: ${file.absolutePath}")
                } else {
                    deletedfiles++
                }
            }
            log.debug("${deletedfiles} files removed")
            log.info("Execution with ID ${e.id} were deleted")
            result = [success: true]
        } catch (Exception ex) {
            log.error("Failed to delete execution ${e.id}", ex)
            result = [error:'failure',message: "Failed to delete execution {{Execution ${e.id}}}: ${ex.message}", success: false]
        }
        return result
    }

    private List<Execution> searchExecutions(FrameworkService frameworkService, ExecutionService executionService, String project, Integer maxDaysToKeep,
                                             Integer minimumExecutionToKeep, Integer maximumDeletionSize = 500){
        List collectedExecutions= []

        if(frameworkService.isClusterModeEnabled()){
            log.info("searching executions of node ID: ${frameworkService.getServerUUID()}")
        }

        Map jobList = executionService.queryExecutions(createCriteria(
                project,
                maxDaysToKeep,
                maximumDeletionSize,
                frameworkService.isClusterModeEnabled()?frameworkService.getServerUUID():null))

        if(null != jobList && null != jobList.get("total")) {
            Integer totalToExclude = (Integer) jobList.get("total")
            log.info("found ${totalToExclude} executions")
            if(totalToExclude >0) {
                List<Execution> result = ((List<Execution>)jobList.get("result")).sort{a,b -> b.dateCompleted <=> a.dateCompleted}
                if(minimumExecutionToKeep > 0){
                    int totalExecutions = this.totalAllExecutions(executionService, project)
                    int sub = totalExecutions - totalToExclude
                    log.info("minimum executions to keep: ${minimumExecutionToKeep}")
                    log.info("total exections of project ${project}: ${totalExecutions}")
                    log.info("total to exclude: ${totalToExclude}")
                    if(sub < minimumExecutionToKeep) {
                        int jump = minimumExecutionToKeep - sub
                        log.info("${jump} executions can not be removed")
                        result = jump < result.size() ? result[jump..totalToExclude - 1] : []
                        log.info("${result.size()} executions will be removed")
                    }
                }
                for (Execution exec: result) {
                    if(exec.getStatus() != null) { //exclude running executions
                        collectedExecutions.add(exec)
                        log.info(exec.toString())
                    }else{
                        log.info("Running execution: ${exec.toString()}")
                    }                }
            }
        }
        if(collectedExecutions.size()==0){
            log.info("No executions to delete")
        }
        return collectedExecutions
    }

    private int totalAllExecutions(ExecutionService executionService, String project){
        ExecutionQuery query = new ExecutionQuery(projFilter: project)
        Map result = executionService.queryExecutions(query)
        return null != result ? result.total : 0
    }

    private Closure createCriteria(String project, Integer maxDaysToKeep = 0, Integer maxDetetionSize = 500, String serverNodeUUID = null){
        Date endDate=ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d")
        return {isCount ->
            if(serverNodeUUID){
                eq('serverNodeUUID', serverNodeUUID)
            } else {
                isNull('serverNodeUUID')
            }

            eq('project', project)
            le('dateCompleted', endDate)
            maxResults(maxDetetionSize)
            if (!isCount) {
                and {
                    order('dateCompleted', 'asc')
                    order('dateStarted', 'asc')

                }
            }
        }
    }

    private int deleteByExecutionList(List<Execution> collectedExecutions, FileUploadService fileUploadService, LogFileStorageService logFileStorageService) {
        log.info("Start to delete ${collectedExecutions.size()} executions")
        if(collectedExecutions.size()>0) {
            Map result = deleteBulkExecutionIds(collectedExecutions, fileUploadService, logFileStorageService)
            if (result != null) {
                List failureList = new ArrayList<>();
                List<Map> resultList = (List<Map>) result.get("failures")
                if(resultList != null) {
                    for (Map res : resultList) {
                        if (!(Boolean) result.get("success")) {
                            failureList.add((String) res.get("message"))
                            log.info(res.get("message"))
                        }
                    }
                }
                Integer successTotal = (Integer) result.get("successTotal")
                log.info("Deleted ${successTotal} of ${collectedExecutions.size()} executions")
                if(successTotal<collectedExecutions.size()){
                    log.error("Some executions weren't deleted")
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
}
