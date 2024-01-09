package org.rundeck.app.data.job.converters

import com.fasterxml.jackson.databind.ObjectMapper
import rundeck.Orchestrator
import rundeck.data.job.RdOrchestrator

class OrchestratorUpdater {
    static ObjectMapper mapper = new ObjectMapper()

    static void updateOrchestrator(Orchestrator orchestrator, RdOrchestrator rdo) {
        orchestrator.type = rdo.type
        orchestrator.content = mapper.writeValueAsString(rdo.configuration)
    }
}
