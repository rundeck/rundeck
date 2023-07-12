package org.rundeck.app.data.job.converters

import rundeck.Orchestrator
import rundeck.data.job.RdOrchestrator
import spock.lang.Specification

class OrchestratorToRdOrchestratorConverterSpec extends Specification {
    def "should return null when input is null"() {
        given:
        OrchestratorToRdOrchestratorConverter converter = new OrchestratorToRdOrchestratorConverter()

        when:
        RdOrchestrator result = converter.convertOrchestrator(null)

        then:
        result == null
    }

    def "should return RdOrchestrator with type and configuration when input is valid"() {
        given:
        OrchestratorToRdOrchestratorConverter converter = new OrchestratorToRdOrchestratorConverter()
        Orchestrator input = new Orchestrator(type: "limitRun", configuration: [key: "value"])

        when:
        RdOrchestrator result = converter.convertOrchestrator(input)

        then:
        result.type == input.type
        result.configuration == input.configuration
    }
}
