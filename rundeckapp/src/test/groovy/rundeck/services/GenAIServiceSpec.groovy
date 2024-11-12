package rundeck.services

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class GenAIServiceSpec extends Specification implements ServiceUnitTest<GenAIService> {

    def 'test getJobDescriptionFromJobDefinition with successful response'() {
        given:
        def sample_job_definition = """
  defaultTab: nodes
  description: Job One 5
  executionEnabled: true
  id: 48601def-8838-4201-a1a7-1f2e6c46a762
  loglevel: INFO
  name: J1
  nodeFilterEditable: false
  plugins:
    ExecutionLifecycle: {}
  scheduleEnabled: true
  sequence:
    commands:
    - exec: echo Hello
    keepgoing: false
    strategy: node-first
  uuid: 48601def-8838-4201-a1a7-1f2e6c46a762
"""

        when:
        def result = service.getJobDescriptionFromJobDefinition(sample_job_definition)

        then:
        !result.isEmpty()
    }
}