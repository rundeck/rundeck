package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin
import grails.test.*
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.services.logging.EventStreamingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState

class LogFileStorageServiceTests extends GrailsUnitTestCase {
    File testLogFile1
    File testLogFileDNE
    protected void setUp() {
        super.setUp()

        testLogFile1 = File.createTempFile("LogFileStorageServiceTests", ".txt")
        testLogFile1.deleteOnExit()
        testLogFileDNE = File.createTempFile("LogFileStorageServiceTests", ".txt")
        testLogFileDNE.delete()
    }

    protected void tearDown() {
        super.tearDown()
        if(testLogFile1.exists()){
            testLogFile1.delete()
        }
    }

    void testConfiguredPluginName() {
        LogFileStorageService svc = new LogFileStorageService()

        ConfigurationHolder.config = [:]
        assertNull(svc.getConfiguredPluginName())


        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        assertEquals("test1", svc.getConfiguredPluginName())
    }
    void testConfiguredRetry() {
        LogFileStorageService svc = new LogFileStorageService()

        ConfigurationHolder.config = [:]
        assertEquals(60,svc.getConfiguredStorageRetryDelay())
        assertEquals(1,svc.getConfiguredStorageRetryCount())


        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retryDelay = 30

        assertEquals(30, svc.getConfiguredStorageRetryDelay())
        assertEquals(1, svc.getConfiguredStorageRetryCount())

        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retryDelay = 30
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retryCount = 10

        assertEquals(30, svc.getConfiguredStorageRetryDelay())
        assertEquals(10, svc.getConfiguredStorageRetryCount())
    }

    void testgetLogFileWriterWithoutPlugin(){
        mockDomain(Execution)
        mockLogging(LogFileStorageService)
        ConfigurationHolder.config = [:]
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testproj"
        }
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService=fmock.createMock()
        svc.frameworkService.initialized=true
        svc.frameworkService.rundeckbase='/tmp'

        def writer = svc.getLogFileWriterForExecution(e, [:])
        assertNotNull(writer)
        assert writer instanceof FSStreamingLogWriter
    }
    class testStoragePlugin implements LogFileStoragePlugin{
        Map<String, ? extends Object> context
        LogFileState state
        boolean initializeCalled
        boolean storeLogFileCalled
        boolean storeLogFileSuccess
        boolean retrieveLogFileCalled
        boolean retrieveLogFileSuccess

        @Override
        void initialize(Map<String, ? extends Object> context) {
            initializeCalled=true
            this.context=context;
        }

        @Override
        LogFileState getState() {
            return state
        }

        @Override
        boolean store(InputStream stream, long length, Date lastModified) throws IOException {
            storeLogFileCalled = true
            return storeLogFileSuccess
        }

        @Override
        boolean retrieve(OutputStream stream) throws IOException {
            retrieveLogFileCalled=true
            return retrieveLogFileSuccess
        }

        public assertStoreLogFileCalled(){
            assert storeLogFileCalled
        }
        public assertRetrieveLogFileCalled(){
            assert retrieveLogFileCalled
        }
    }
    void testgetLogFileWriterWithPlugin(){
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()

        def execution = createExecution()
        def writer=performWriterRequest(test, execution)

        assertNotNull(writer)
        assert writer instanceof EventStreamingLogWriter
        EventStreamingLogWriter elogwriter = (EventStreamingLogWriter) writer
        assertNotNull(elogwriter.onClose)
        assertNotNull(elogwriter.writer)
        assert elogwriter.writer instanceof FSStreamingLogWriter

        //context set from execution data
        assert test.initializeCalled
        assert test.context!=null

        assertEquals(1, LogFileStorageRequest.list().size())
        LogFileStorageRequest req = LogFileStorageRequest.list().first()
        assertEquals(false, req.completed)
        assertEquals(execution, req.execution)
        assertEquals("test1", req.pluginName)
    }

    void testPluginLogFileWriterOnCloseShouldStartStorageRequest(){
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        LogFileStorageService svc
        def writer=performWriterRequest(test, createExecution()){LogFileStorageService service->
            svc=service
            assertEquals(0, svc.getCurrentStorageRequests().size())
        }
        writer.close()
        assertEquals(1, svc.getCurrentStorageRequests().size())
    }

    private StreamingLogWriter performWriterRequest(testStoragePlugin test, Execution e, Closure clos=null) {
        mockDomain(Execution)
        mockDomain(LogFileStorageRequest)
        mockLogging(LogFileStorageService)
        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testprojz"
        }
        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(1) { pname, svc, resolv, scope ->
            assertEquals("test1", pname)
            assert scope == PropertyScope.Instance
            test
        }
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService = fmock.createMock()
        svc.frameworkService.initialized = true
        svc.frameworkService.rundeckbase = '/tmp'
        svc.pluginService = pmock.createMock()

        assertEquals(0, LogFileStorageRequest.list().size())
        if(null!=clos){
            svc.with(clos)
        }
        return svc.getLogFileWriterForExecution(e, [:])

    }

    void testRequestLogFileReaderFileDNE(){

        ConfigurationHolder.config = [:]

        def test = null

        def e = createExecution()

        def reader = performReaderRequest(test, false, testLogFileDNE, false, e)
        assertNotNull(reader)
        assertEquals(ExecutionLogState.NOT_FOUND,reader.state)
        assertNull(reader.reader)
    }

    void testRequestLogFileReaderFileDNEWaiting() {

        ConfigurationHolder.config = [:]

        def test = null

        def e = createExecution {
            it.dateStarted = new Date()
            it.outputfilepath = null
        }

        def reader = performReaderRequest(test, false, testLogFileDNE, false, e)
        assertNotNull(reader)
        assertEquals(ExecutionLogState.WAITING, reader.state)
        assertNull(reader.reader)
    }
    void testRequestLogFileReaderFileDNEClusterModePendingRemote() {

        ConfigurationHolder.config = [:]

        def test = new testStoragePlugin()
        test.state = LogFileState.NOT_FOUND

        def e = createExecution{
            it.dateStarted = new Date()
            it.outputfilepath = null
        }

        def reader = performReaderRequest(test, true, testLogFileDNE, false, e)

        assertNotNull(reader)
        assertEquals(ExecutionLogState.PENDING_REMOTE, reader.state)
        assertNull(reader.reader)
    }
    void testRequestLogFileReaderFileExists(){

        ConfigurationHolder.config = [:]

        def test = new testStoragePlugin()
        test.state = LogFileState.NOT_FOUND

        def reader = performReaderRequest(test, false, testLogFile1, false, createExecution())

        //initialize should not have been called
        assert !test.initializeCalled

        assertNotNull(reader)
        assertEquals(ExecutionLogState.AVAILABLE, reader.state)
        assertNotNull(reader.reader)
        assertTrue(reader.reader instanceof FSStreamingLogReader)
    }


    void testRequestLogFileReaderFileDNEPluginStateNotFoundShouldResultInPendingRemote() {
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.state=LogFileState.NOT_FOUND

        def reader = performReaderRequest(test, false, testLogFileDNE, false, createExecution())

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.PENDING_REMOTE, reader.state)
        assertNull(reader.reader)
    }
    void testRequestLogFileReaderFileDNEPluginStatePendingShouldResultInPendingRemote() {
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.state = LogFileState.PENDING

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution())

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.PENDING_REMOTE, reader.state)
        assertNull(reader.reader)
    }
    void testRequestLogFileReaderFileDNEPluginStateAvailableShouldResultInAvailableRemote() {
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.state = LogFileState.AVAILABLE

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution())

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.AVAILABLE_REMOTE, reader.state)
        assertNull(reader.reader)
    }
    void testRequestLogFileReaderFileDNEPluginRequestAlreadyPending() {
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.state = LogFileState.AVAILABLE

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution()){ LogFileStorageService svc->
            svc.metaClass.logFileRetrievalRequestState={Execution execution->
                ExecutionLogState.PENDING_LOCAL
            }
        }

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.PENDING_LOCAL, reader.state)
        assertNull(reader.reader)
    }
    void testRequestLogFileReaderFileDNEStartsANewRequest() {
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.state = LogFileState.AVAILABLE

        def LogFileStorageService service
        def reader=performReaderRequest(test, false, testLogFileDNE, true, createExecution()){ LogFileStorageService svc->
            service=svc
            assert svc.getCurrentRetrievalRequests().size()==0
        }
        assert service.getCurrentRetrievalRequests().size()==1

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.PENDING_LOCAL, reader.state)
        assertNull(reader.reader)
    }

    private ExecutionLogReader performReaderRequest(test, boolean isClustered, File logfile, boolean performLoad, Execution e, Closure svcClosure=null) {
        mockDomain(Execution)
        mockDomain(LogFileStorageRequest)
        mockLogging(LogFileStorageService)

        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testprojz"
        }
        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(1) { pname, svc, resolv, scope ->
            assertEquals("test1", pname)
            assert scope == PropertyScope.Instance
            test
        }
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService = fmock.createMock()
        svc.frameworkService.initialized = true
        svc.frameworkService.rundeckbase = '/tmp'
        svc.frameworkService.serverUUID = null
        svc.frameworkService.metaClass.isClusterModeEnabled = {
            return isClustered
        }
        svc.frameworkService.metaClass.getServerUUID = {
            UUID.randomUUID()
        }
        svc.pluginService = pmock.createMock()
        svc.metaClass.getFileForExecution = { Execution e2 ->
            assert e == e2
            logfile
        }
        if(null!= svcClosure){
            svc.with(svcClosure)
        }

        return svc.requestLogFileReader(e, performLoad)
    }

    private Execution createExecution(Closure clos=null) {
        mockDomain(Execution)
        def e = new Execution(argString: "-test args", user: "testuser", project: "testprojz", loglevel: 'WARN', doNodedispatch: false)
        if(null!=clos){
            e.with(clos)
        }
        return e
    }

}
