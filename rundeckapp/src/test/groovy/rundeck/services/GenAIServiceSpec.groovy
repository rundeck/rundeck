package rundeck.services

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class GenAIServiceSpec extends Specification implements ServiceUnitTest<GenAIService> {

    def 'test getJobDescriptionFromJobDefinition with successful response'() {
        given:
        def sample_job_definition = """
<joblist>
  <job>
    <defaultTab>nodes</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <id>48601def-8838-4201-a1a7-1f2e6c46a762</id>
    <loglevel>INFO</loglevel>
    <name>Job that prints messages to stdout</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <plugins />
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <exec>echo Hi</exec>
      </command>
      <command>
        <exec>echo Bye</exec>
      </command>
    </sequence>
    <uuid>48601def-8838-4201-a1a7-1f2e6c46a762</uuid>
  </job>
</joblist>
"""
        def apiKey = System.getenv("OPENAI_API_KEY")

        when:
        def result = service.getJobDescriptionFromJobDefinition(apiKey, sample_job_definition)

        then:
        !result.isEmpty()
    }
}