package rundeck.data.execution

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.execution.ExecutionData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
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
    Serializable internalId
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
    String workflowJson

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
        workflowJson(nullable:true)
        nodeConfig(nullable: true)
        orchestrator(nullable: true)
        retryExecutionId(nullable: true)
        retryOriginalId(nullable: true)
        retryPrevId(nullable: true)
        extraMetadataMap(nullable: true)
    }

    @Override
    WorkflowData getWorkflowData() {
        // Backwards compatibility: check old workflow field first
        if (workflow != null) {
            return workflow
        }

        // New format: deserialize from JSON
        if (workflowJson != null) {
            return deserializeWorkflowData(workflowJson)
        }

        return null
    }

    /**
     * Deserialize workflow JSON to WorkflowData.
     * Converts JSON -> Map -> Workflow domain object for runtime use.
     * @param json JSON string
     * @return WorkflowData instance
     */
    private WorkflowData deserializeWorkflowData(String json) {
        if (json == null || json.isEmpty()) {
            return null
        }

        try {
            // Deserialize JSON to Map using EmbeddedJsonData trait
            Map workflowMap = asJsonMap(json)

            // Convert Map to Workflow domain object for runtime type safety
            return RdWorkflow.fromMap(workflowMap)
        } catch (Exception e) {
            log.error("Failed to deserialize workflowJson for Execution ${id}", e)
            return null
        }
    }

    static Map asJsonMap(String data) {
        //de-serialize the json
        if (null != data) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(data, Map.class)
        } else {
            return null
        }
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
