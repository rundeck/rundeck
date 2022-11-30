package org.rundeck.app.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData
import rundeck.Orchestrator

@JsonIgnoreProperties(["errors"])
class RdOrchestrator implements OrchestratorData, Validateable {
    Long id
    String type
    Map<String,Object> configuration

    static constraints = {
        importFrom(Orchestrator)
        id(nullable: true)
    }
}
