import grails.test.GrailsUnitTestCase
import com.dtolabs.rundeck.util.ZipBuilder
import rundeck.Execution
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.ExecReport
import rundeck.BaseReport
import rundeck.services.ProjectService
import rundeck.services.ScheduledExecutionService
/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 
/*
 * ProjectServiceTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 10/16/12 9:57 AM
 * 
 */
class ProjectServiceTests extends GrailsUnitTestCase {
    static String EXEC_XML_TEST1_START = '''<executions>
  <execution id='1'>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>'''
    static String EXEC_XML_TEST1_REST = '''
    <failedNodeList />
    <abortedby />
    <cancelled>false</cancelled>
    <argString>-test args</argString>
    <loglevel>WARN</loglevel>
    <doNodedispatch>true</doNodedispatch>
    <nodefilters>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
        <excludePrecedence>true</excludePrecedence>
        <rankOrder>ascending</rankOrder>
      </dispatch>
      <include>
        <hostname>test1</hostname>
      </include>
      <exclude>
        <tags>monkey</tags>
      </exclude>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <exec>exec command</exec>
      </command>
    </workflow>
  </execution>
</executions>'''
    static String EXEC_XML_TEST1 = EXEC_XML_TEST1_START+ '''
    <outputfilepath />''' + EXEC_XML_TEST1_REST

    /**
     * Execution xml output with an output file path
     */
    static String EXEC_XML_TEST2 = EXEC_XML_TEST1_START+ '''
    <outputfilepath>output-1.txt</outputfilepath>''' + EXEC_XML_TEST1_REST
    def testExportExecution(){
        mockDomain(ScheduledExecution)
        mockDomain(Execution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        ProjectService svc = new ProjectService()

        def outfilename = "blahfile.xml"

        def zipmock=mockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1){name,Closure withwriter->
            assertEquals(outfilename,name)
            withwriter.call(outwriter)
            outwriter.flush()
        }
//        zipmock.demand.file(1..1){name,File outfile-> }
        def zip = zipmock.createMock()
        Execution exec = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: true,
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                nodeInclude: 'test1',
                nodeExcludeTags: 'monkey',
                status: 'true',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')])
        )
        assertNotNull exec.save()

        svc.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        assertEquals EXEC_XML_TEST1, str
    }
    def testExportExecutionOutputFile(){
        mockDomain(ScheduledExecution)
        mockDomain(Execution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        ProjectService svc = new ProjectService()

        def outfilename = "blahfile.xml"
        File tempoutfile = File.createTempFile("tempout",".txt")

        def zipmock=mockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1){name,Closure withwriter->
            assertEquals(outfilename,name)
            withwriter.call(outwriter)
            outwriter.flush()
        }
//        zipmock.demand.file(1..1){name,File outfile-> }

        Execution exec = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: true,
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                nodeInclude: 'test1',
                nodeExcludeTags: 'monkey',
                status: 'true',
                outputfilepath: tempoutfile.absolutePath,
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'exec command')])
        )
        assertNotNull exec.save()

        zipmock.demand.file(1..1) {name, File out ->
            assertEquals('output-'+exec.id+'.txt', name)
            assertEquals(tempoutfile,out)
        }
        def zip = zipmock.createMock()
        svc.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        assertEquals EXEC_XML_TEST2, str
    }
    def testImportExecution(){
        mockDomain(ScheduledExecution)
        mockDomain(Execution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        ProjectService svc = new ProjectService()
        def result = svc.loadExecutions(EXEC_XML_TEST1)
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()
        def Execution e = result.executions[0]
        def expected = [
                argString: '-test args',
                user: 'testuser',
                project: 'testproj',
                loglevel: 'WARN',
                doNodedispatch: true,
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                nodeInclude: 'test1',
                nodeExcludeTags: 'monkey',
                status: 'true',
        ]
        assertPropertiesEquals expected,e
        assertEquals( [(e):1],result.execidmap)

        assertNotNull e.workflow
        assertNotNull e.workflow.commands
        assertEquals 1,e.workflow.commands.size()
        assertPropertiesEquals( [adhocRemoteString: 'exec command'],e.workflow.commands[0])
    }
    def testImportExecutionRemappedJob(){
        mockDomain(ScheduledExecution)
        mockDomain(Execution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        def testJobId='test-id1'

        def newJobId = 'test-id2'
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: newJobId,
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where',
                                                       description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                                                       )
        assertNotNull se.save()
        def idMap=[(testJobId):newJobId]


        def semock = mockFor(ScheduledExecutionService)
        semock.demand.getByIDorUUID(1..1){id->
            assertEquals(newJobId,id)
            se
        }

        ProjectService svc = new ProjectService()
        svc.scheduledExecutionService=semock.createMock()

        def result = svc.loadExecutions(EXEC_XML_TEST1_START+"<outputfilepath/><jobId>${testJobId}</jobId>"+EXEC_XML_TEST1_REST,idMap)
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()

        def Execution e = result.executions[0]
        def expected = [
                argString: '-test args',
                user: 'testuser',
                project: 'testproj',
                loglevel: 'WARN',
                doNodedispatch: true,
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                nodeInclude: 'test1',
                nodeExcludeTags: 'monkey',
                status: 'true',
        ]
        assertPropertiesEquals expected,e
        assertNotNull(e.scheduledExecution)
        assertEquals(se,e.scheduledExecution)
        assertEquals( [(e):1],result.execidmap)

        assertNotNull e.workflow
        assertNotNull e.workflow.commands
        assertEquals 1,e.workflow.commands.size()
        assertPropertiesEquals( [adhocRemoteString: 'exec command'],e.workflow.commands[0])
    }
    def assertPropertiesEquals(Map data, Object obj){
        data.each{k,v->
            def test=obj[k]
            if(!(v.class.isAssignableFrom(test.class))){
                fail("Expected value of type ${v.class}, but value was ${test.class}")
            }
            assert v==test, "unexpected value ${test} for key ${k}"
        }
    }

    static String REPORT_XML_TEST1='''<report>
  <node>1/0/0</node>
  <title>blah</title>
  <status>succeed</status>
  <actionType>succeed</actionType>
  <ctxProject>testproj1</ctxProject>
  <ctxType />
  <ctxName />
  <reportId>test/job</reportId>
  <tags>a,b,c</tags>
  <author>admin</author>
  <message>Report message</message>
  <dateStarted>1970-01-01T00:00:00Z</dateStarted>
  <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
  <ctxCommand />
  <ctxController>ct</ctxController>
  <jcExecId>123</jcExecId>
  <jcJobId>test-job-uuid</jcJobId>
  <adhocExecution />
  <adhocScript />
  <abortedByUser />
</report>'''
    def testExportReport() {
        mockDomain(BaseReport)
        mockDomain(ExecReport)
        mockDomain(ScheduledExecution)

        def newJobId = 'test-job-uuid'
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                uuid: newJobId,
                adhocFilepath: '/this/is/a/path', groupPath: 'some/where', description: 'a job', argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                )
        assertNotNull se.save()
        def oldJobId=se.id
        ProjectService svc = new ProjectService()

        def outfilename = "reportout.xml"

        def zipmock = mockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1) {name, Closure withwriter ->
            assertEquals(outfilename, name)
            withwriter.call(outwriter)
            outwriter.flush()
        }
        def zip = zipmock.createMock()
        ExecReport exec = new ExecReport(
                ctxController:'ct',
                 jcExecId:'123',
                 jcJobId: oldJobId.toString(),
                 node:'1/0/0',
                 title: 'blah',
                 status: 'succeed',
                 actionType: 'succeed',
                 ctxProject: 'testproj1',
                 reportId: 'test/job',
                 tags: 'a,b,c',
                 author: 'admin',
                 dateStarted: new Date(0),
                 dateCompleted: new Date(3600000),
                 message: 'Report message',
        )
        assertNotNull exec.save()

        svc.exportHistoryReport(zip, exec, outfilename)
        def str = outwriter.toString()
        println str
        assertEquals REPORT_XML_TEST1, str
    }

    def testLoadReport() {
        mockDomain(BaseReport)
        mockDomain(ExecReport)
        mockDomain(ScheduledExecution)

        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: 'new-job-uuid',
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where', description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                                                       )
        assertNotNull se.save()
        def newJobId = se.id
        def oldUuid= 'test-job-uuid'

        ProjectService svc = new ProjectService()
        def ExecReport result = svc.loadHistoryReport(REPORT_XML_TEST1,[(123):'456'],[(oldUuid):se],'test')
        assertNotNull result
        def expected = [
                ctxController: 'ct',
                jcExecId: '456',
                jcJobId: newJobId.toString(),
                node: '1/0/0',
                title: 'blah',
                status: 'succeed',
                actionType: 'succeed',
                ctxProject: 'testproj1',
                reportId: 'test/job',
                tags: 'a,b,c',
                author: 'admin',
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                message: 'Report message',
        ]
        assertPropertiesEquals expected, result
    }
    def testReportRoundtrip() {
        mockDomain(BaseReport)
        mockDomain(ExecReport)
        mockDomain(ScheduledExecution)
        ProjectService svc = new ProjectService()

        def outfilename = "reportout.xml"

        def zipmock = mockFor(ZipBuilder)
        def outwriter = new StringWriter()
        zipmock.demand.file(1..1) {name, Closure withwriter ->
            assertEquals(outfilename, name)
            withwriter.call(outwriter)
            outwriter.flush()
        }
        def zip = zipmock.createMock()
        ExecReport exec = new ExecReport(
                ctxController: 'ct',
                jcExecId: '123',
                jcJobId: '321',
                node: '1/0/0',
                title: 'blah',
                status: 'succeed',
                actionType: 'succeed',
                ctxProject: 'testproj1',
                reportId: 'test/job',
                tags: 'a,b,c',
                author: 'admin',
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                message: 'Report message',
                )
        assertNotNull exec.save()

        svc.exportHistoryReport(zip, exec, outfilename)
        def str = outwriter.toString()

        def ExecReport result = svc.loadHistoryReport(str,null,null,'test')
        assertNotNull result
        def keys = [
                ctxController: 'ct',
                jcExecId: '123',
                jcJobId: '321',
                node: '1/0/0',
                title: 'blah',
                status: 'succeed',
                actionType: 'succeed',
                ctxProject: 'testproj1',
                reportId: 'test/job',
                tags: 'a,b,c',
                author: 'admin',
                dateStarted: new Date(0),
                dateCompleted: new Date(3600000),
                message: 'Report message',
        ].keySet()
        assertPropertiesEquals exec.properties.subMap(keys), result
    }
}
