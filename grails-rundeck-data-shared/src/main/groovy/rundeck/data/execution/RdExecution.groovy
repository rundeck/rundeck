package rundeck.data.execution

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.execution.ExecutionData
import rundeck.data.constants.ExecutionConstants
import rundeck.data.job.RdNodeConfig
import rundeck.data.job.RdOrchestrator
import rundeck.data.job.RdWorkflow
import rundeck.data.util.ExecutionDataUtil
import rundeck.data.validation.shared.SharedExecutionConstraints
import rundeck.data.validation.shared.SharedNodeConfigConstraints
import rundeck.data.validation.shared.SharedProjectNameConstraints
import rundeck.data.validation.shared.SharedServerNodeUuidConstraints

@JsonIgnoreProperties(["errors","executionState"])
class RdExecution implements ExecutionData, Validateable {
    String uuid = UUID.randomUUID().toString()
    String jobUuid
    Date dateStarted
    Date dateCompleted
    String project
    String argString
    String status
    String loglevel
    String outputfilepath
    String failedNodeList
    String succeededNodeList
    String abortedby
    boolean cancelled
    Boolean timedOut=false
    String timeout
    String retry
    String retryDelay

    String executionType
    Integer retryAttempt=0
    Boolean willRetry=false
    String user
    List<String> userRoles
    String serverNodeUUID
    Integer nodeThreadcount=1
    Long logFileStorageRequestId
    Long retryExecutionId
    Long retryOriginalId
    Long retryPrevId
    Map<String, Object> extraMetadataMap

    RdWorkflow workflow
    RdOrchestrator orchestrator
    RdNodeConfig nodeConfig

    boolean serverNodeUUIDChanged = false //transient

    static constraints = {
        importFrom SharedProjectNameConstraints
        importFrom SharedExecutionConstraints
        importFrom SharedServerNodeUuidConstraints
        importFrom SharedNodeConfigConstraints
        logFileStorageRequestId(nullable:true)
        workflow(nullable:true)
        nodeConfig(nullable: true)
        orchestrator(nullable: true)
        retryExecutionId(nullable: true)
        retryOriginalId(nullable: true)
        retryPrevId(nullable: true)
        extraMetadataMap(nullable: true)
    }

    @Override
    String getExecutionState() {
        return ExecutionDataUtil.getExecutionState(this)
    }

    @Override
    boolean statusSucceeded(){
        return getExecutionState()== ExecutionConstants.EXECUTION_SUCCEEDED
    }
}
