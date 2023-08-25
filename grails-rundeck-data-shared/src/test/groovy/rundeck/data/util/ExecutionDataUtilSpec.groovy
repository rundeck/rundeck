package rundeck.data.util

import rundeck.data.constants.ExecutionConstants
import rundeck.data.execution.RdExecution
import spock.lang.Specification

class ExecutionDataUtilSpec extends Specification {

    def "test getExecutionState method"() {
        expect:
        ExecutionDataUtil.getExecutionState(executionData) == expectedExecutionState

        where:
        executionData                    | expectedExecutionState
        new RdExecution(cancelled: true) | ExecutionConstants.EXECUTION_ABORTED
        new RdExecution(status: ExecutionConstants.EXECUTION_QUEUED)      | ExecutionConstants.EXECUTION_QUEUED
        new RdExecution(dateStarted: new Date(System.currentTimeMillis()+5000)) | ExecutionConstants.EXECUTION_SCHEDULED
        new RdExecution(dateCompleted: null, status: ExecutionConstants.AVERAGE_DURATION_EXCEEDED) | ExecutionConstants.AVERAGE_DURATION_EXCEEDED
        new RdExecution(dateCompleted: null, status: "running")           | ExecutionConstants.EXECUTION_RUNNING
        new RdExecution(dateCompleted: new Date(),status: "succeeded")    | ExecutionConstants.EXECUTION_SUCCEEDED
        new RdExecution(dateCompleted: new Date(), cancelled: true)       | ExecutionConstants.EXECUTION_ABORTED
        new RdExecution(dateCompleted: new Date(),willRetry: true)        | ExecutionConstants.EXECUTION_FAILED_WITH_RETRY
        new RdExecution(dateCompleted: new Date(),timedOut: true)         | ExecutionConstants.EXECUTION_TIMEDOUT
        new RdExecution(dateCompleted: new Date(),status: "missed")       | ExecutionConstants.EXECUTION_MISSED
        new RdExecution(dateCompleted: new Date(),status: "failed")       | ExecutionConstants.EXECUTION_FAILED
        new RdExecution(dateCompleted: new Date(),status: "custom")       | ExecutionConstants.EXECUTION_STATE_OTHER

        // Add more test cases as needed
    }

    def "test isCustomStatusString method"() {
        expect:
        ExecutionDataUtil.isCustomStatusString(value) == expectedResult

        where:
        value                                                      | expectedResult
        ExecutionConstants.EXECUTION_TIMEDOUT                      | false
        ExecutionConstants.EXECUTION_FAILED_WITH_RETRY             | false
        ExecutionConstants.EXECUTION_ABORTED                       | false
        ExecutionConstants.EXECUTION_SUCCEEDED                     | false
        ExecutionConstants.EXECUTION_FAILED                        | false
        ExecutionConstants.EXECUTION_QUEUED                        | false
        ExecutionConstants.EXECUTION_SCHEDULED                     | false
        ExecutionConstants.AVERAGE_DURATION_EXCEEDED               | false
        "custom"                                                   | true
        "other"                                                    | true

    }
}
