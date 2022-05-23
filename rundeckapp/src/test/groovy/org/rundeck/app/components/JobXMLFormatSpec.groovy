package org.rundeck.app.components

import org.rundeck.app.components.jobs.JobFormat
import spock.lang.Specification
import spock.lang.Unroll

class JobXMLFormatSpec extends Specification {
    @Unroll
    def "should return a notification map with email and webhook notifs"() {
        given:
        def input = "" +
        "<joblist>\n" +
        "  <job>\n" +
        "    <defaultTab>nodes</defaultTab>\n" +
        "    <description></description>\n" +
        "    <loglevel>INFO</loglevel>\n" +
        "    <name>a</name>\n" +
        "    <nodeFilterEditable>false</nodeFilterEditable>\n" +
        "    <notification>\n" +
        "      <onsuccess>\n" +
        "        <email attachLog='true' attachLogInFile='true' recipients='leojesus.juarez@gmail.com' subject='RD-SUCCESS' />\n" +
        "        <webhook format='xml' httpMethod='get' urls='http://localhost:4440/project' />\n" +
        "        <webhook format='json' httpMethod='post' urls='http://localhost:4440/project' />\n" +
        "        <email attachLog='true' attachLogInline='true' recipients='2@gmail.com' subject='RD-SUCCESS' />\n" +
        "      </onsuccess>\n" +
        "    </notification>\n" +
        "    <notifyAvgDurationThreshold />\n" +
        "    <plugins />\n" +
        "    <scheduleEnabled>true</scheduleEnabled>\n" +
        "    <schedules />\n" +
        "    <sequence keepgoing='false' strategy='node-first'>\n" +
        "      <command>\n" +
        "        <exec>asd</exec>\n" +
        "      </command>\n" +
        "    </sequence>\n" +
        "  </job>\n" +
        "</joblist>"
        def sut = new JobXMLFormat()
        when:
        def result = sut.decode(new StringReader(input))
        then:
        result[0].notification['onsuccess'].size() == 4
        result[0].notification['onsuccess'].findAll{ it['email'] != null }.size() == 2
        result[0].notification['onsuccess'].findAll{ it['urls'] != null }.size() == 2
    }

    @Unroll
    def "should return an xml str with email and webhook tags"() {
        given:
        List<Map> input = [[
                id: 0,
                defaultTab: "nodes",
                description: "",
                loglevel: "INFO",
                name: "a",
                nodeFilterEditable: false,
                notification: [
                        onsuccess: [
                                [email: [recipients: "mail@example.com"]],
                                [email: [recipients: "mail2@example.com"]],
                                [
                                        format: "xml",
                                        httpMethod: "get",
                                        urls: "http://example1.com/1"
                                ],
                                [
                                        format: "json",
                                        httpMethod: "post",
                                        urls: "https://example2.com/2"
                                ]
                        ]
                ],
                notifyAvgDurationThreshold: "",
                plugins: "",
                scheduledEnabled: "true",
                schedules: "",
                sequence: [
                        keepgoing: false,
                        strategy: "node-first",
                        commands: [[exec:"asd"]]
                ],
                executionEnabled: true,
                multipleExecutions: false
        ]]
        def sut = new JobXMLFormat()
        def writer = new StringWriter()
        def options = JobFormat.options(true, [:], (String) null)

        when:
        sut.encode(input, options, writer)
        String notifStr = writer.toString()
        notifStr = notifStr.substring(notifStr.indexOf("<onsuccess>") + "<onsuccess>".length())
        notifStr = notifStr.substring(0,notifStr.indexOf("</onsuccess>")).replaceAll("\\s","")

        then:
        notifStr.contains("<emailrecipients='mail@example.com'/>")
        notifStr.contains("<emailrecipients='mail2@example.com'/>")
        notifStr.contains("<webhookformat='xml'httpMethod='get'urls='http://example1.com/1'/>")
        notifStr.contains("<webhookformat='json'httpMethod='post'urls='https://example2.com/2'/>")
    }
}
