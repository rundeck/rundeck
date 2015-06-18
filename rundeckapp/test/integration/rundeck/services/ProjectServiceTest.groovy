package rundeck.services

import grails.test.mixin.TestFor
import rundeck.Execution

/**
 * Created by greg on 6/18/15.
 */
class ProjectServiceTest extends GroovyTestCase {
    def ProjectService projectService
    def sessionFactory


    static String EXECS_START='<executions>'
    static String EXEC_XML_TEST1_DEF_START= '''
  <execution id='1'>
    <dateStarted>1970-01-01T00:00:00Z</dateStarted>
    <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
    <status>true</status>'''
    static String EXEC_XML_TEST1_START = EXECS_START+EXEC_XML_TEST1_DEF_START
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
</executions>''' /**
     * Execution xml with orchestrator
     */
    static String EXEC_XML_TEST5 = EXEC_XML_TEST1_START + '''
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

    <orchestrator>
      <type>subset</type>
      <configuration>
        <count>1</count>
      </configuration>
    </orchestrator>
  </execution>
</executions>'''

    /**
     * import executions with orchestrator definition
     */
    public void  testImportExecutionsToProject_Workflow_noOutfile(){
        def temp = File.createTempFile("execxml",".tmp")
        temp.text=EXEC_XML_TEST4
        temp.deleteOnExit()
        def errs=[]
        def result
        Execution.withNewSession {
            result  = projectService.importExecutionsToProject([temp], [:], "test", null, [:], [], [(temp): "temp-xml-file"], errs)
            sessionFactory.currentSession.flush()
        }
        assertEquals("expected no errors but saw: ${errs}", 1,errs.size())
        assertTrue(errs[0].contains("NO matching outfile"))
    }
    /**
     * import executions with retryExecution link
     */
    public void  testImportExecutionsToProject_retryId(){
        def id2 = 13L
        def id1 = 1L
        def temp = File.createTempFile("execxml",".tmp")
        temp.text = """
<executions>
      <execution id='${id2}'>
        <dateStarted>1970-01-01T00:00:00Z</dateStarted>
        <dateCompleted>1970-01-01T01:00:00Z</dateCompleted>
        <status>true</status><outputfilepath />""" + EXEC_XML_TEST1_DEF_END + "</executions>"

        temp.deleteOnExit()

        def temp2 = File.createTempFile("execxml2",".tmp")
        temp2.text = EXECS_START + EXEC_XML_TEST1_DEF_START +
                '''<retryExecutionId>13</retryExecutionId> <outputfilepath />''' +
                EXEC_XML_TEST1_DEF_END +
                "</executions>"

        temp2.deleteOnExit()
        def errs=[]
        Map result
        Execution.withNewSession {
            result  = projectService.importExecutionsToProject([temp,temp2], [:], "test", null, [:], [], [(temp): "temp-xml-file"], errs)
            sessionFactory.currentSession.flush()
        }
        assertEquals("expected no errors but saw: ${errs}", 2,errs.size())
        assertTrue(errs[0].contains("NO matching outfile"))
        assertTrue(errs[1].contains("NO matching outfile"))
        assertEquals(2,result.size())
        System.err.println("result: ${result}")

        assertNotNull(result[(int)id1])
        assertNotNull(result[(int)id2])

        assertNotNull(Execution.get(result[(int)id1]))
        assertNotNull(Execution.get(result[(int)id2]))
        def exec=Execution.get(result[(int)id1])
        assertNotNull(exec.retryExecution)
        def exec2=Execution.get(result[(int)id2])
        assertEquals(exec2,exec.retryExecution)
    }
    /**
     * import executions with orchestrator definition
     */
    public void  testImportExecutionsToProject_Workflow_withOutfile(){
        def temp = File.createTempFile("execxml",".tmp")
        temp.text=EXEC_XML_TEST4
        temp.deleteOnExit()
        def outfile = File.createTempFile("output",".tmp")
        outfile.text="bah"
        outfile.deleteOnExit()
        def resultoutfile = File.createTempFile("newoutput",".tmp")
        resultoutfile.deleteOnExit()
        assertTrue resultoutfile.delete()
        def errs=[]
        def result
        projectService.logFileStorageService.metaClass.getFileForExecutionFiletype={Execution execution, String filetype, boolean useStoredPath->
            resultoutfile
        }
        Execution.withNewSession {
            result  = projectService.importExecutionsToProject([temp], ["output-1.rdlog":outfile], "test", null, [:], [], [(temp): "temp-xml-file"], errs)
            sessionFactory.currentSession.flush()
        }

        assertEquals("expected no errors but saw: ${errs}", 0,errs.size())
        assertEquals("bah",resultoutfile.text)
        resultoutfile.deleteOnExit()
    }
    /**
     * import executions with orchestrator definition
     */
    public void  testImportExecutionsToProject_Workflow_withStatefile(){
        def temp = File.createTempFile("execxml",".tmp")
        temp.text=EXEC_XML_TEST4
        temp.deleteOnExit()
        def outfile = File.createTempFile("output",".tmp")
        outfile.text="bah"
        outfile.deleteOnExit()
        def statefile = File.createTempFile("statefile",".tmp")
        statefile.text="state file contents"
        statefile.deleteOnExit()
        def resultoutfile = File.createTempFile("newoutput",".tmp")
        resultoutfile.deleteOnExit()
        assertTrue resultoutfile.delete()
        def resultstatefile = File.createTempFile("newstate",".tmp")
        resultstatefile.deleteOnExit()
        assertTrue resultstatefile.delete()
        def files = [
                "state.json":resultstatefile,
                "rdlog":resultoutfile
        ]
        def errs=[]
        def result
        projectService.logFileStorageService.metaClass.getFileForExecutionFiletype={Execution execution, String filetype, boolean useStoredPath->
            files[filetype]
        }
        Execution.withNewSession {
            result  = projectService.importExecutionsToProject([temp], ["output-1.rdlog":outfile,"state-1.state.json":statefile], "test", null, [:], [], [(temp): "temp-xml-file"], errs)
            sessionFactory.currentSession.flush()
        }

        assertEquals("expected no errors but saw: ${errs}", 0,errs.size())
        assertEquals("bah",resultoutfile.text)
        assertEquals("state file contents",resultstatefile.text)
        resultoutfile.deleteOnExit()
        resultstatefile.deleteOnExit()
    }
    /**
     * import executions with orchestrator definition
     */
    public void  testImportExecutionsToProject_Orchestrator(){
        def temp = File.createTempFile("execxml",".tmp")
        temp.text=EXEC_XML_TEST5
        temp.deleteOnExit()
        def errs=[]
        def result
        Execution.withNewSession {
            result = projectService.importExecutionsToProject([temp], [:], "test", null, [:], [], [(temp): "temp-xml-file"], errs)
            sessionFactory.currentSession.flush()
        }
        assertEquals("expected no errors but saw: ${errs}", 1,errs.size())
        assertTrue(errs[0].contains("NO matching outfile"))
    }
}
