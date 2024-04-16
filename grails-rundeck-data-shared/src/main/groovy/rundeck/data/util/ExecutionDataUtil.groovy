package rundeck.data.util

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobReference
import org.rundeck.app.data.model.v1.execution.ExecutionData
import rundeck.data.constants.ExecutionConstants
import rundeck.data.execution.reference.ExecutionReferenceImpl

class ExecutionDataUtil {

    public static String getExecutionState(ExecutionData e) {
        return e.cancelled ? ExecutionConstants.EXECUTION_ABORTED :
                (null == e.dateCompleted && e.status == ExecutionConstants.EXECUTION_QUEUED) ? ExecutionConstants.EXECUTION_QUEUED :
                        null != e.dateStarted && e.dateStarted.getTime() > System.currentTimeMillis() ? ExecutionConstants.EXECUTION_SCHEDULED :
                                (null == e.dateCompleted && e.status!=ExecutionConstants.AVERAGE_DURATION_EXCEEDED) ? ExecutionConstants.EXECUTION_RUNNING :
                                        (e.status == ExecutionConstants.AVERAGE_DURATION_EXCEEDED) ? ExecutionConstants.AVERAGE_DURATION_EXCEEDED:
                                                (e.status in ['true', 'succeeded']) ? ExecutionConstants.EXECUTION_SUCCEEDED :
                                                        e.cancelled ? ExecutionConstants.EXECUTION_ABORTED :
                                                                e.willRetry ? ExecutionConstants.EXECUTION_FAILED_WITH_RETRY :
                                                                        e.timedOut ? ExecutionConstants.EXECUTION_TIMEDOUT :
                                                                                (e.status == 'missed') ? ExecutionConstants.EXECUTION_MISSED :
                                                                                        (e.status in ['false', 'failed']) ? ExecutionConstants.EXECUTION_FAILED :
                                                                                                isCustomStatusString(e.status) ? ExecutionConstants.EXECUTION_STATE_OTHER : e.status.toLowerCase()

    }

    public static boolean isCustomStatusString(String value){
        null!=value && !(value.toLowerCase() in [ExecutionConstants.EXECUTION_TIMEDOUT,
                                                 ExecutionConstants.EXECUTION_FAILED_WITH_RETRY,
                                                 ExecutionConstants.EXECUTION_ABORTED,
                                                 ExecutionConstants.EXECUTION_SUCCEEDED,
                                                 ExecutionConstants.EXECUTION_FAILED,
                                                 ExecutionConstants.EXECUTION_QUEUED,
                                                 ExecutionConstants.EXECUTION_SCHEDULED,
                                                 ExecutionConstants.AVERAGE_DURATION_EXCEEDED])
    }

    public static ExecutionReference createExecutionReference(ExecutionData executionData, JobReference jobRef = null, String targetNodes = null) {
        String adhocCommand = null
        if(!jobRef) {
            adhocCommand = executionData.workflow.steps.get(0).summarize()
        }
        return new ExecutionReferenceImpl(
                project: executionData.project,
                id: executionData.internalId?.toString(),
                uuid: executionData.uuid,
                retryOriginalId: executionData.retryOriginalId?.toString(),
                retryPrevId: executionData.retryPrevId?.toString(),
                retryNextId: executionData.retryExecutionId?.toString(),
                options: executionData.argString,
                filter: executionData.nodeConfig?.filter,
                job: jobRef,
                adhocCommand: adhocCommand,
                dateStarted: executionData.dateStarted,
                status: executionData.status,
                succeededNodeList: executionData.succeededNodeList,
                failedNodeList: executionData.failedNodeList,
                targetNodes: targetNodes,
                metadata: executionData.extraMetadataMap,
                scheduled: executionData.executionType in ['scheduled','user-scheduled'],
                executionType: executionData.executionType
        )
    }
}
