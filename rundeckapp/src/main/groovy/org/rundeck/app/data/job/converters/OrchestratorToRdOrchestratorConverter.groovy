package org.rundeck.app.data.job.converters

import rundeck.Orchestrator
import rundeck.data.job.RdOrchestrator

class OrchestratorToRdOrchestratorConverter {

    static RdOrchestrator convertOrchestrator(Orchestrator o) {
        if(!o) return null
        RdOrchestrator orchestrator = new RdOrchestrator()
        orchestrator.type = o.type
        orchestrator.configuration = o.configuration
        return orchestrator
    }
}
