package org.rundeck.app.data.job.converters

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData
import rundeck.Orchestrator

class OrchestratorUpdater {
    static ObjectMapper mapper = new ObjectMapper()

    static void updateOrchestrator(Orchestrator orchestrator, OrchestratorData rdo) {
        orchestrator.type = rdo.type
        orchestrator.content = mapper.writeValueAsString(rdo.configuration)
    }
}
