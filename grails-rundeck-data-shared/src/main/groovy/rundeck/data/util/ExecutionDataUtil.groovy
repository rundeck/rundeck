package rundeck.data.util

import org.rundeck.app.data.model.v1.execution.ExecutionData
import rundeck.data.constants.ExecutionConstants

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
}
