package rundeck.services

import spock.lang.Specification

class JobSimilarityDetectionServiceSpec extends Specification {

    static final def jd1 = """
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

    static final def jd2 = """
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
    </sequence>
    <uuid>48601def-8838-4201-a1a7-1f2e6c46a762</uuid>
  </job>
</joblist>
"""

    static final def jd3 = """
<joblist>
  <job>
    <defaultTab>nodes</defaultTab>
    <description>J1</description>
    <executionEnabled>true</executionEnabled>
    <id>a342ec64-7fac-4fdf-a8b1-c27f2a597a1a</id>
    <loglevel>INFO</loglevel>
    <name>J1</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <plugins />
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <description>Create file</description>
        <exec>echo "Test content" &gt; /tmp/test_source_1.txt</exec>
      </command>
      <command>
        <description>Copy file</description>
        <node-step-plugin type='copyfile'>
          <configuration>
            <entry key='destinationPath' value='/tmp/test_dest_1.txt' />
            <entry key='echo' value='true' />
            <entry key='recursive' value='false' />
            <entry key='sourcePath' value='/tmp/test_source_1.txt' />
          </configuration>
        </node-step-plugin>
      </command>
      <command>
        <description>View copied file content</description>
        <exec>cat /tmp/test_dest_1.txt</exec>
      </command>
    </sequence>
    <uuid>a342ec64-7fac-4fdf-a8b1-c27f2a597a1a</uuid>
  </job>
</joblist>
"""

    def "test areSimilar with strings"() {
        given:
        BigDecimal threshold = 0.8

        expect:
        JobSimilarityDetectionService.areSimilar(jd1, jd2, threshold)
        def j1 = JobSimilarityDetectionService.jaccard(jd1, jd2)
        j1 != null

        JobSimilarityDetectionService.areSimilar(jd1, jd3, threshold) == false
        def j2 = JobSimilarityDetectionService.jaccard(jd1, jd3)
        j2 != null
    }

}
