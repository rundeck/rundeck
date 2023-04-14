package org.rundeck.app.data.providers

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution

class GormReferencedExecutionDataProvider implements ReferencedExecutionDataProvider{
    @Override
    Long updateOrCreateReference(Long refId, Long jobId, Long execId, String status) {
        if(refId){
            ReferencedExecution refExec = ReferencedExecution.findById(refId)
            refExec.status = status
            refExec.save(flush:true)
            return refId
        }else{
            ScheduledExecution se = ScheduledExecution.findById(jobId)
            Execution exec = Execution.findById(execId)
            ReferencedExecution refExec = new ReferencedExecution(
                    scheduledExecution: se, execution: exec, status: status).save(flush:true)
            return refExec.id
        }
    }

    @Override
    RdReferencedExecution findByJobId(Long jobId) {
        def se = ScheduledExecution.findById(jobId)
        return ReferencedExecution.findByScheduledExecution(se, [max: 1])
    }

    @Override
    List<Long> parentList(Long jobId, int max) {
        def se = ScheduledExecution.findById(jobId)
        return ReferencedExecution.parentListScheduledExecutionId(se, max)
    }

    @Override
    List executionProjectList(Long jobId, int max = 0) {
        def se = ScheduledExecution.findById(jobId)
        return ReferencedExecution.executionProjectList(se, max)
    }

    @Override
    void deleteByExecutionId(Long id) {
        def execution = Execution.findById(id)
        ReferencedExecution.findAllByExecution(execution).each{ re ->
            re.delete()
        }

    }

    @Override
    void deleteByJobId(Long id) {
        def se = ScheduledExecution.findById(id)
        ReferencedExecution.findAllByScheduledExecution(se).each {re ->
            re.delete()
        }
    }

    @Override
    int countByJobId(Long jobId) {
        def se = ScheduledExecution.findById(jobId)
        return ReferencedExecution.countByScheduledExecution(se)
    }

    @Override
    int countByJobIdAndStatus(Long jobId, String status) {
        def se = ScheduledExecution.findById(jobId)
        return ReferencedExecution.countByScheduledExecutionAndStatus(se, status)
    }
}
