import com.dtolabs.rundeck.util.ZipBuilder
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Execution
import rundeck.Option
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.ExecReport
import rundeck.BaseReport
import rundeck.services.LoggingService
import rundeck.services.ProjectService
import rundeck.services.ScheduledExecutionService
import rundeck.services.WorkflowService

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
@TestFor(ProjectService)
@Mock([ScheduledExecution, Option, Workflow, CommandExec, Execution,BaseReport, ExecReport])
class ProjectServiceTests  {
    static String EXECS_START='<executions>'
    static String EXECS_END= '</executions>'
    static String EXEC_XML_TEST1_DEF_START= '''
  <execution id='1'>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>'''
    static String EXEC_XML_TEST1_DEF_END= '''
    <failedNodeList />
    <succeededNodeList />
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
      <filter>hostname: test1 !tags: monkey</filter>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <exec>exec command</exec>
      </command>
    </workflow>
  </execution>
'''

    static String EXEC_XML_TEST1_START = EXECS_START+EXEC_XML_TEST1_DEF_START
    static String EXEC_XML_TEST1_REST = EXEC_XML_TEST1_DEF_END+EXECS_END
    static String EXEC_XML_TEST1 = EXEC_XML_TEST1_START+ '''
    <outputfilepath />''' + EXEC_XML_TEST1_REST

    /**
     * Execution xml output with an output file path
     */
    static String EXEC_XML_TEST2 = EXEC_XML_TEST1_START+ '''
    <outputfilepath>output-1.rdlog</outputfilepath>''' + EXEC_XML_TEST1_REST

    /**
     * Execution xml with associated job ID
     */
    static String EXEC_XML_TEST3 = EXEC_XML_TEST1_START + '''
    <outputfilepath />''' + '''
    <jobId>jobid1</jobId>''' + EXEC_XML_TEST1_REST
    /**
     * Execution xml with associated job ID
     */
    static String EXEC_XML_TEST4 = EXEC_XML_TEST1_START + '''
    <outputfilepath>output-1.rdlog</outputfilepath>''' + '''
    <failedNodeList />
    <succeededNodeList />
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
      <filter>hostname: test1 !tags: monkey</filter>
    </nodefilters>
    <project>testproj</project>
    <user>testuser</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <jobref name='echo' nodeStep='true'>
          <arg line='-name ${node.name}' />
        </jobref>
        <description>echo on node</description>
      </command>
    </workflow>
  </execution>
</executions>'''

    public void testExportExecution(){
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
        def logmock = mockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1){Execution e->
            assert exec==e
            new File(outfilename)
        }
        svc.loggingService=logmock.createMock()
        def workflowmock = mockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1){Execution e->
            assert exec==e
            null
        }
        svc.workflowService= workflowmock.createMock()

        svc.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        assertEquals EXEC_XML_TEST1, str
    }
    public void  testExportExecutionOutputFile(){
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
            assertEquals('output-'+exec.id+'.rdlog', name)
            assertEquals(tempoutfile,out)
        }
        def zip = zipmock.createMock()

        def logmock = mockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1) { Execution e ->
            assert exec == e
            tempoutfile
        }
        svc.loggingService = logmock.createMock()
        def workflowmock = mockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1) { Execution e ->
            assert exec == e
            null
        }

        svc.workflowService = workflowmock.createMock()
        svc.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        assertEquals EXEC_XML_TEST2, str
    }
    public void  testExportExecutionStateFile(){
        ProjectService svc = new ProjectService()

        def outfilename = "blahfile.xml"
        File tempoutfile = File.createTempFile("tempout",".txt")
        File tempoutfile2 = File.createTempFile("tempout",".state.json")

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
        int filecalled=0
        zipmock.demand.file(2..2) {name, File out ->
            filecalled++
            if(filecalled==1){
                assertEquals('output-'+exec.id+'.rdlog', name)
                assertEquals(tempoutfile,out)
            }else{
                assertEquals('state-' + exec.id + '.state.json', name)
                assertEquals(tempoutfile2, out)
            }
        }
        def zip = zipmock.createMock()

        def logmock = mockFor(LoggingService)
        logmock.demand.getLogFileForExecution(1..1) { Execution e ->
            assert exec == e
            tempoutfile
        }
        svc.loggingService = logmock.createMock()
        def workflowmock = mockFor(WorkflowService)
        workflowmock.demand.getStateFileForExecution(1..1) { Execution e ->
            assert exec == e
            tempoutfile2
        }
        svc.workflowService = workflowmock.createMock()

        svc.exportExecution(zip,exec,outfilename)
        def str=outwriter.toString()
//        println str
        assertEquals(2, filecalled)
        assertEquals EXEC_XML_TEST2, str
    }
    public void  testImportExecution(){
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
                filter: 'hostname: test1 !tags: monkey',
                status: 'true',
        ]
        assertPropertiesEquals expected,e
        assertEquals e,e
        assertEquals 1,result.execidmap.size()
        assertEquals e,result.execidmap.keySet().first()
        assertEquals 1,result.execidmap.values().first()
        assertEquals( [(e):1],result.execidmap)

        assertNotNull e.workflow
        assertNotNull e.workflow.commands
        assertEquals 1,e.workflow.commands.size()
        assertPropertiesEquals( [adhocRemoteString: 'exec command'],e.workflow.commands[0])
    }
    public void  testImportExecutionWorkflow(){
        ProjectService svc = new ProjectService()
        def result = svc.loadExecutions(EXEC_XML_TEST4)
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 1,result.executions.size()
        def Execution e = result.executions[0]
        assertNotNull e.workflow
        assertNotNull e.workflow.commands
        assertEquals 1,e.workflow.commands.size()
        assertPropertiesEquals( [jobName: 'echo', nodeStep:true,argString: '-name ${node.name}',
                description: 'echo on node'],
                e.workflow.commands[0])
    }
    /**
     * Imported execution where jobId should be skipped, should not be loaded
     */
    public void  testImportExecutionSkipJob(){
        ProjectService svc = new ProjectService()
        def result = svc.loadExecutions(EXEC_XML_TEST3,null,['jobid1'])
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertEquals 0,result.executions.size()
        assertEquals 0,result.execidmap.size()
    }
    public void  testImportExecutionRemappedJob(){
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
                filter: 'hostname: test1 !tags: monkey',
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
    public void  testloadExecutionsRetryExecId(){
        def remapExecId='12'
        def idMap=[:]


        def semock = mockFor(ScheduledExecutionService)
        semock.demand.getByIDorUUID(1..1){id->
            assertEquals(newJobId,id)
            se
        }

        ProjectService svc = new ProjectService()
        svc.scheduledExecutionService=semock.createMock()

        def result = svc.loadExecutions(
                EXECS_START
                    + EXEC_XML_TEST1_DEF_START
                    + '''<retryExecutionId>12</retryExecutionId> <outputfilepath />'''
                    + EXEC_XML_TEST1_DEF_END
                    + '''
  <execution id='12'>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>'''
                    + ''' <outputfilepath />'''
                    + EXEC_XML_TEST1_DEF_END
                + EXECS_END
                ,idMap)
        assertNotNull result
        assertNotNull result.executions
        assertNotNull result.execidmap
        assertNotNull result.retryidmap
        assertEquals 1,result.retryidmap.size()
        assertEquals 12,result.retryidmap.values().first()
        assertEquals 2,result.executions.size()

    }
    public void  assertPropertiesEquals(Map data, Object obj){
        data.each{k,v->
            def test=obj[k]
            if(null==test){
                fail("key:'${k}' Expected value '${v}' of type ${v.class}, but value was null")
            }
            if(!(v.class.isAssignableFrom(test.class))){
                fail("key:'${k}' Expected value of type ${v.class}, but value was ${test.class}")
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
  <reportId>test/job</reportId>
  <tags>a,b,c</tags>
  <author>admin</author>
  <message>Report message</message>
  <dateStarted>1970-01-01T00:00:00Z</dateStarted>
  <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
  <jcExecId>123</jcExecId>
  <jcJobId>test-job-uuid</jcJobId>
  <adhocExecution />
  <adhocScript />
  <abortedByUser />
</report>'''
    public void  testExportReport() {

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

    public void  testLoadReport() {

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
    public void  testLoadReportSkippedExecution() {

        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', project: 'AProject', adhocExecution: true,
                                                       uuid: 'new-job-uuid',
                                                       adhocFilepath: '/this/is/a/path', groupPath: 'some/where', description: 'a job', argString: '-a b -c d',
                                                       workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                                                       )
        assertNotNull se.save()
        def newJobId = se.id
        def oldUuid= 'test-job-uuid'

        ProjectService svc = new ProjectService()
        def ExecReport result = svc.loadHistoryReport(REPORT_XML_TEST1,[:],[(oldUuid):se],'test')
        assertNull result
    }
    public void  testReportRoundtrip() {
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

        def ExecReport result = svc.loadHistoryReport(str,[(123):123],null,'test')
        assertNotNull result
        def keys = [
                jcExecId: '456',
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
