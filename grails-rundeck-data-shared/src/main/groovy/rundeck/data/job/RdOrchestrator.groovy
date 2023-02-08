package rundeck.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData

@JsonIgnoreProperties(["errors"])
class RdOrchestrator implements OrchestratorData, Validateable {
    String type
    Map<String,Object> configuration

    static constraints = {
        type(nullable:false,blank:false)
    }
}
