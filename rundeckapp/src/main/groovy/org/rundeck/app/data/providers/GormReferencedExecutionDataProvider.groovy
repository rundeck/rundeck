package org.rundeck.app.data.providers

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution

class GormReferencedExecutionDataProvider implements ReferencedExecutionDataProvider{
    @Override
    Long updateOrCreateReference(Long refId, Long seId, Long execId, String status) {
        if(refId){
            ReferencedExecution refExec = ReferencedExecution.findById(refId)
            refExec.status = status
            refExec.save(flush:true)
            return refId
        }else{
            ScheduledExecution se = ScheduledExecution.findById(seId)
            Execution exec = Execution.findById(execId)
            ReferencedExecution refExec = new ReferencedExecution(
                    scheduledExecution: se, execution: exec, status: status).save(flush:true)
            return refExec.id
        }
    }

    @Override
    RdReferencedExecution findByScheduledExecutionId(Long seId) {
        def se = ScheduledExecution.findById(seId)
        return ReferencedExecution.findByScheduledExecution(se, [max: 1])
    }

    @Override
    List<Long> parentList(Long seId, int max) {
        def se = ScheduledExecution.findById(seId)
        return ReferencedExecution.parentListScheduledExecutionId(se, max)
    }

    @Override
    List executionProjectList(Long seId, int max = 0) {
        def se = ScheduledExecution.findById(seId)
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
    void deleteByScheduledExecutionId(Long id) {
        def se = ScheduledExecution.findById(id)
        ReferencedExecution.findAllByScheduledExecution(se).each {re ->
            re.delete()
        }
    }

    @Override
    int countByScheduledExecution(Long seId) {
        def se = ScheduledExecution.findById(seId)
        return ReferencedExecution.countByScheduledExecution(se)
    }

    @Override
    int countByScheduledExecutionAndStatus(Long seId, String status) {
        def se = ScheduledExecution.findById(seId)
        return ReferencedExecution.countByScheduledExecutionAndStatus(se, status)
    }
}
