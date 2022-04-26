package com.rundeck.plugin.jobs


import com.rundeck.plugin.ExecutionModeService
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnableToInterruptJobException

class SystemExecutionModeJob implements InterruptableJob {
    @Override
    void interrupt() throws UnableToInterruptJobException {

    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {

        Map config = context.jobDetail.jobDataMap.get('config')
        ExecutionModeService executionModeService = fetchExecutionModeService(context.jobDetail.jobDataMap)

        config.active = false

        boolean active = true
        if(config.action == "disable"){
            active=false
        }

        executionModeService.setExecutionsAreActive(active)


        config.action = null
        config.value = null

        def storagePath = ExecutionModeService.EXECUTION_MODE_STORAGE_PATH_BASE + "executionModeLater.properties"
        executionModeService.saveConfig(storagePath, config)


    }

    private ExecutionModeService fetchExecutionModeService(def jobDataMap) {
        def es = jobDataMap.get("executionModeService")
        if (es==null) {
            throw new RuntimeException("ExecutionModeService could not be retrieved from JobDataMap!")
        }
        if (! (es instanceof ExecutionModeService)) {
            throw new RuntimeException("JobDataMap contained invalid ExecutionModeService type: " + es.getClass().getName())
        }
        return es

    }
}
