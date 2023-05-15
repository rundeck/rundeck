package org.rundeck.app.data.providers

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution
import rundeck.services.JobSchedulerService

class GormReferencedExecutionDataProvider implements ReferencedExecutionDataProvider{
    @Override
    Long updateOrCreateReference(Long refId, String jobUuid, Long execId, String status) {
        if(refId){
            ReferencedExecution refExec = ReferencedExecution.findById(refId)
            refExec.status = status
            refExec.save(flush:true)
            return refId
        }else{
            Execution exec = Execution.findById(execId)
            ReferencedExecution refExec = new ReferencedExecution(
                    jobUuid: jobUuid, execution: exec, status: status).save(flush:true)
            return refExec.id
        }
    }

    @Override
    RdReferencedExecution findByJobUuid(String jobUuid) {
        return ReferencedExecution.findByJobUuid(jobUuid, [max: 1])
    }

    @Override
    List<JobDataSummary> parentJobSummaries(String jobUuid, int max) {
        return ReferencedExecution.parentJobSummaries(jobUuid, max)
    }

    @Override
    List<String> parentList(String jobUuid, int max) {
        return ReferencedExecution.parentListScheduledExecutionUuid(jobUuid, max)
    }

    @Override
    List<String> executionProjectList(String jobUuid, int max = 0) {
        return ReferencedExecution.executionProjectList(jobUuid, max)
    }

    @Override
    void deleteByExecutionId(Long id) {
        def execution = Execution.findById(id)
        ReferencedExecution.findAllByExecution(execution).each{ re ->
            re.delete()
        }

    }

    @Override
    void deleteByJobUuid(String jobUuid) {
        ReferencedExecution.findAllByJobUuid(jobUuid).each {re ->
            re.delete()
        }
    }

    @Override
    int countByJobUuid(String jobUuid) {
        return ReferencedExecution.countByJobUuid(jobUuid)
    }

    @Override
    int countByJobUuidAndStatus(String jobUuid, String status) {
        return ReferencedExecution.countByJobUuidAndStatus(jobUuid, status)
    }
}
