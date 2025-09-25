package rundeck.quartzjobs

import com.dtolabs.rundeck.app.support.ExecutionQuery
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.plugins.metricsweb.MetricService
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnableToInterruptJobException
import rundeck.Execution
import rundeck.services.*
import com.codahale.metrics.Timer

import java.util.concurrent.Callable

@CompileStatic
@Slf4j
class ExecutionsCleanUp implements InterruptableJob {
    boolean wasInterrupted

    void interrupt() throws UnableToInterruptJobException {
        wasInterrupted = true
    }

    void execute(JobExecutionContext context) throws JobExecutionException {
        FrameworkService frameworkService = fetchFrameworkService(context.jobDetail.jobDataMap)
        MetricService metricService = fetchMetricService(context.jobDetail.jobDataMap)
        ExecutionService executionService = fetchExecutionService(context.jobDetail.jobDataMap)

        Timer timer = metricService.timer("rundeck.quartzjobs.ExecutionsCleanUp", "executionCleanup")

        String project = context.jobDetail.jobDataMap.get('project')
        String uuid = frameworkService.getServerUUID()

        log.info("Initializing cleaner execution history job from server ${uuid}")

        String maxDaysToKeep = context.jobDetail.jobDataMap.get('maxDaysToKeep')
        String minimumExecutionToKeep = context.jobDetail.jobDataMap.get('minimumExecutionToKeep')
        String maximumDeletionSize = context.jobDetail.jobDataMap.get('maximumDeletionSize')

        log.info("Cleaner parameters: Project name: ${project}")
        log.info("Max days to keep: ${maxDaysToKeep}")
        log.info("Minimum executions to keep: ${minimumExecutionToKeep}")
        log.info("Maximum size of deletions: ${maximumDeletionSize ?: '500 (default)'}")


        if(!wasInterrupted) {

            timer.time(
                (Callable) {
                    List<Long> execIdsToExclude = searchExecutions(
                            executionService,
                            project,
                            maxDaysToKeep ? Integer.parseInt(maxDaysToKeep) : 0,
                            minimumExecutionToKeep ? Integer.parseInt(minimumExecutionToKeep) : 0,
                            maximumDeletionSize ? Integer.parseInt(maximumDeletionSize) : 500)
                    log.info("Executions to delete: ${execIdsToExclude.size()}")
                    deleteByExecutionList(execIdsToExclude, executionService)
                    log.info("Finished cleaner execution history job from server ${uuid}")
                }
            )
        }
    }

    private static class DeleteExecutionResult {
        boolean success
        String error
        String message
        Long executionId

        DeleteExecutionResult(boolean success, String error, String message, Long executionId) {
            this.success = success
            this.error = error
            this.message = message
            this.executionId = executionId
        }
        static DeleteExecutionResult success(Long executionId) {
            return new DeleteExecutionResult(true, null, null, executionId)
        }
        static DeleteExecutionResult failure(String error, String message, Long executionId) {
            return new DeleteExecutionResult(false, error, message, executionId)
        }
    }

    @CompileDynamic
    private DeleteExecutionResult deleteExecution(Execution execution,
                                                 FileUploadService fileUploadService,
                                                 LogFileStorageService logFileStorageService,
                                                 ReferencedExecutionDataProvider referencedExecutionDataProvider,
                                                 ReportService reportService,
                                                 ExecutionService executionService){
        try {
            if (execution.dateCompleted == null && execution.dateStarted != null) {
                return DeleteExecutionResult.failure('running', "Failed to delete execution {{Execution ${execution.id}}}: The execution is currently running", execution.id)
            }

            referencedExecutionDataProvider.deleteByExecutionId(execution.id)
            reportService.deleteByExecution(execution)
            def executionFiles = logFileStorageService.getExecutionFiles(execution, [], false)

            List<File> files = []
            def execs = []
            execs << execution
            executionFiles.each { ftype, executionFile ->
                def localFile = logFileStorageService.getFileForExecutionFiletype(execution, ftype, false, false)
                if (null != localFile && localFile.exists()) {
                    files << localFile
                }
                def partialFile = logFileStorageService.getFileForExecutionFiletype(execution, ftype, false, true)
                if (null != partialFile && partialFile.exists()) {
                    files << partialFile
                }
                def resultDeleteRemote = logFileStorageService.removeRemoteLogFile(execution, ftype)
                if(!resultDeleteRemote.started){
                    log.debug(String.valueOf(resultDeleteRemote.error))
                }
            }
            fileUploadService.deleteRecordsForExecution(execution)
            log.debug("${files.size()} files from execution will be deleted")
            //find an execution that this is a retry for
            List<Execution> retryExecutions = Execution.findAll("from Execution where retryExecution = :exec", [exec: execution])
            retryExecutions.each { Execution e2 ->
                e2.retryExecution = null
            }
            execution.delete(flush: true)
            def deletedfiles = 0
            files.each { file ->
                if (!FileUtils.deleteQuietly(file)) {
                    log.warn("Failed to delete file while deleting execution ${execution.id}: ${file.absolutePath}")
                } else {
                    deletedfiles++
                }
            }
            log.debug("${deletedfiles} files removed")
            log.info("Deleted execution: ${execution.id}")
            return DeleteExecutionResult.success(execution.id)
        } catch (Exception ex) {
            log.error("Failed to delete execution ${execution?.id}", ex)
            return DeleteExecutionResult.failure('failure', "Failed to delete execution {{Execution ${execution?.id}}}: ${ex.message}", execution?.id)
        }
    }

    private List<Long> searchExecutions(ExecutionService executionService,
                                             String project,
                                             Integer maxDaysToKeep,
                                             Integer minimumExecutionToKeep,
                                             Integer maximumDeletionSize = 500){
        List<Long> collectedExecutions= []

        log.info("Searching All Executions")

        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", null)

        List<Long> jobList = Execution.executeQuery(
            """select e.id from Execution e 
               where e.project = :project 
               and e.dateCompleted <= :endDate 
               order by e.dateCompleted asc""",
            [
                project: project,
                endDate: endDate
            ],
            [max: maximumDeletionSize]
        )
        List<Long> result = executionService.queryExecutionsList(
                getExecutionsQueryCriteria(
                        project,
                        maxDaysToKeep,
                        maximumDeletionSize
                )
        ) as List<Long>

        if(null != jobList && 0 != jobList.size()) {
            List result = jobList
            Integer totalFound = jobList.size()
            Integer totalToExclude = result.size()

            log.info("found ${totalFound} executions")
            if(totalToExclude >0) {
                if (minimumExecutionToKeep > 0) {

                    int totalExecutions = this.totalAllExecutions(project)
                    int sub = totalExecutions - totalToExclude

                    log.info("minimum executions to keep: ${minimumExecutionToKeep}")
                    log.info("total exections of project ${project}: ${totalExecutions}")
                    log.info("total to exclude: ${totalToExclude}")

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
                        log.info("${jump} executions can not be removed")
                        //remove the oldest executions
                        def finalIndex = result.size() - jump
                        result = jump < result.size() ? result.subList(0, finalIndex) : []
                        log.info("${result.size()} executions will be removed")
                    }

                    if(!remove){
                        log.info("${result.size()} executions can not be removed ")
                        result = []
                    }
                }

                collectedExecutions.addAll(result)
            }
        }


        if(collectedExecutions.size()==0){
            log.info("No executions to delete")
        }
        return collectedExecutions
    }

    private int totalAllExecutions(String project){
        Integer total = Execution.executeQuery(
                "select count(e.id) from Execution e where e.project = :project",
                [project: project]
        )[0]
        return null != total ? total : 0
    }


    private int deleteByExecutionList(List<Long> collectedExecutions,
                                      ExecutionService executionService) {
        log.info("Start to delete ${collectedExecutions.size()} executions")
        int count = 0
        if (collectedExecutions.size() > 0) {
            for (Long execId : collectedExecutions) {
                Map result = executionService.deleteExecutionFromCleanup(execId)
                if (!result.success) {
                    log.error(result.message as String)
                } else {
                    count++
                }
            }
            log.info("Deleted ${count} of ${collectedExecutions.size()} executions")

            if (count < collectedExecutions.size()) {
                log.error("Some executions weren't deleted")
            }
            return count
        }
        return 0
    }

    @CompileDynamic
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

    @CompileDynamic
    private FrameworkService fetchFrameworkService(def jobDataMap) {
        def fws = jobDataMap.get("frameworkService")
        if (fws==null) {
            throw new RuntimeException("frameworkService could not be retrieved from JobDataMap!")
        }
        if (! (fws instanceof FrameworkService)) {
            throw new RuntimeException("JobDataMap contained invalid frameworkService type: " + fws.getClass().getName())
        }
        return (FrameworkService)fws
    }

    @CompileDynamic
    private MetricService fetchMetricService(def jobDataMap) {
        def metricService = jobDataMap.get("metricService")
        if (metricService==null) {
            throw new RuntimeException("metricService could not be retrieved from JobDataMap!")
        }
        if (! (metricService instanceof MetricService)) {
            throw new RuntimeException("JobDataMap contained invalid MetricService type: " + metricService.getClass().getName())
        }
        return (MetricService)metricService
    }
}
