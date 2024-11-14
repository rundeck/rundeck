package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.services.ServiceUnitTest
import spock.lang.Ignore
import spock.lang.Specification

class GenAIServiceSpec extends Specification implements ServiceUnitTest<GenAIService> {

    @Ignore
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

        IRundeckProject projectProps = Mock(IRundeckProject) {
            getProperty('project.job-description-gen.gen-ai.key') >> apiKey
        }

        when:
        def result = service.getJobDescriptionFromJobDefinition(projectProps, sample_job_definition)

        then:
        !result.isEmpty()
    }

    @Ignore
    def 'test getJobDiffDescription with successful response'() {
        given:
        def previous_job_definition = """
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
        def updated_job_definition = """
<joblist>
  <job>
    <defaultTab>nodes</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <id>48601def-8838-4201-a1a7-1f2e6c46a762</id>
    <loglevel>INFO</loglevel>
    <name>Job that prints messages to stdout and logs</name>
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
      <command>
        <exec>echo Log</exec>
      </command>
    </sequence>
    <uuid>48601def-8838-4201-a1a7-1f2e6c46a762</uuid>
  </job>
</joblist>
"""
        def apiKey = System.getenv("OPENAI_API_KEY")

        IRundeckProject projectProps = Mock(IRundeckProject) {
            getProperty('project.job-description-gen.gen-ai.key') >> apiKey
        }

        when:
        def result = service.getJobDiffDescription(projectProps, previous_job_definition, updated_job_definition)

        then:
        !result.isEmpty()
    }

}