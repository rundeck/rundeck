package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import grails.test.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.services.logging.EventStreamingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState

@TestFor(LogFileStorageService)
@Mock([LogFileStorageRequest,Execution])
class LogFileStorageServiceTests  {
    File testLogFile1
    File testLogFileDNE

    public void setUp() {

        testLogFile1 = File.createTempFile("LogFileStorageServiceTests", ".txt")
        testLogFile1.deleteOnExit()
        testLogFileDNE = File.createTempFile("LogFileStorageServiceTests", ".txt")
        testLogFileDNE.delete()
    }

    public void tearDown() {
        if(null!=testLogFile1 && testLogFile1.exists()){
            testLogFile1.delete()
        }
    }

    void testConfiguredPluginName() {
        grailsApplication.config.clear()

        assertNull(service.getConfiguredPluginName())


        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        assertEquals("test1", service.getConfiguredPluginName())
    }
    void testConfiguredRetry() {

        grailsApplication.config.clear()
        assertEquals(60,service.getConfiguredStorageRetryDelay())
        assertEquals(1, service.getConfiguredStorageRetryCount())


        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStorage.storageRetryDelay = 30

        assertEquals(30, service.getConfiguredStorageRetryDelay())
        assertEquals(1, service.getConfiguredStorageRetryCount())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStorage.storageRetryDelay = 30
        grailsApplication.config.rundeck.execution.logs.fileStorage.storageRetryCount = 10

        assertEquals(30, service.getConfiguredStorageRetryDelay())
        assertEquals(10, service.getConfiguredStorageRetryCount())
    }
    void testConfiguredRetrievalRetry() {

        grailsApplication.config.clear()
        assertEquals(60,service.getConfiguredRetrievalRetryDelay())
        assertEquals(3,service.getConfiguredRetrievalRetryCount())


        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30

        assertEquals(30, service.getConfiguredRetrievalRetryDelay())
        assertEquals(3, service.getConfiguredRetrievalRetryCount())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30
        grailsApplication.config.rundeck.execution.logs.fileStorage.retrievalRetryCount = 10

        assertEquals(30, service.getConfiguredRetrievalRetryDelay())
        assertEquals(10, service.getConfiguredRetrievalRetryCount())
    }
    void testIsCachedItemFresh() {

        grailsApplication.config.clear()
        assertTrue(service.isResultCacheItemFresh([time: new Date(), count: 0]))
        assertTrue(service.isResultCacheItemAllowedRetry([time: new Date(), count: 0]))

        assertFalse(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis()- (61*1000)), count: 0]))
        assertFalse(service.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))


        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30

        assertTrue(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0]))
        assertFalse(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0]))
        assertFalse(service.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))


        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30
        grailsApplication.config.rundeck.execution.logs.fileStorage.retrievalRetryCount = 10

        assertTrue(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0]))
        assertFalse(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0]))
        assertTrue(service.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))
        assertFalse(service.isResultCacheItemAllowedRetry([time: new Date(), count: 10]))
    }
    void testGetFileForLocalPath(){
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        service.frameworkService = fmock.createMock()
        def result = service.getFileForLocalPath("abc")
        assertNotNull(result)
        assertEquals(new File("/tmp/logs/rundeck/abc"),result)
    }
    void testGetFileForLocalPathNotFound(){
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever([:])
        }
        service.frameworkService = fmock.createMock()
        try {
            def result = service.getFileForLocalPath("abc")
            fail("Expected exception")
        } catch (IllegalStateException e) {
            assertEquals("framework.logs.dir is not set in framework.properties", e.message)
        }
    }
    void testCacheResult(){
        grailsApplication.config=[:]
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        def result = service.cacheRetrievalState("1", LogFileState.NOT_FOUND, 0, 'error','errorCode',['errorData'])
        assertNotNull(result)
        assertEquals("1",result.id)
        assertEquals(0, result.count)
        assertEquals('errorCode',result.errorCode)
        assertEquals(['errorData'],result.errorData)
        assertEquals('error',result.error)
        assertEquals('test1',result.name)
        assertEquals(LogFileState.NOT_FOUND,result.state)
        assertEquals(1, service.getCurrentRetrievalResults().size())

        Map result1 = service.getRetrievalCacheResult("1")
        assertNotNull(result1)
    }
    void testCacheResultDefaults(){
        grailsApplication.config=[:]
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        def result = service.cacheRetrievalState("1", LogFileState.NOT_FOUND, 0, 'error', null, null)
        assertNotNull(result)
        assertEquals("1",result.id)
        assertEquals(0, result.count)
        assertEquals('execution.log.storage.retrieval.ERROR',result.errorCode)
        assertEquals(['test1','error'],result.errorData)
        assertEquals('error',result.error)
        assertEquals('test1',result.name)
        assertEquals(LogFileState.NOT_FOUND,result.state)
        assertEquals(1, service.getCurrentRetrievalResults().size())

        Map result1 = service.getRetrievalCacheResult("1")
        assertNotNull(result1)
    }
    void testgetLogFileWriterWithoutPlugin(){
        grailsApplication.config.clear()
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testproj"
        }
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        service.frameworkService=fmock.createMock()

        def writer = service.getLogFileWriterForExecution(e, [:])
        assertNotNull(writer)
        assert writer instanceof EventStreamingLogWriter
    }
    class testStoragePlugin implements ExecutionFileStoragePlugin{
        Map<String, ? extends Object> context
        boolean available
        boolean availableException
        String availableFiletype
        boolean initializeCalled
        boolean storeLogFileCalled
        boolean storeLogFileSuccess
        String storeFiletype
        boolean retrieveLogFileCalled
        boolean retrieveLogFileSuccess
        String retrieveFiletype
        long storeLength
        Date storeLastModified

        @Override
        void initialize(Map<String, ? extends Object> context) {
            initializeCalled=true
            this.context=context;
        }

        @Override
        boolean isAvailable(String filetype) throws ExecutionFileStorageException {
            availableFiletype=filetype
            return isAvailable()
        }

        @Override
        boolean isAvailable() throws ExecutionFileStorageException {
            if(availableException){
                throw new ExecutionFileStorageException("testStoragePlugin.available")
            }
            return available
        }

        @Override
        boolean store(String filetype, InputStream stream, long length, Date lastModified) throws IOException, ExecutionFileStorageException {
            storeFiletype=filetype
            return store(stream,length,lastModified)
        }

        @Override
        boolean store(InputStream stream, long length, Date lastModified) throws IOException {
            storeLogFileCalled = true
            storeLength=length
            storeLastModified=lastModified
            return storeLogFileSuccess
        }

        @Override
        boolean retrieve(String filetype, OutputStream stream) throws IOException, ExecutionFileStorageException {
            retrieveFiletype=filetype
            return retrieve(stream)
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
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

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
        assertEquals("rdlog", req.filetype)
    }

    void testPluginLogFileWriterOnCloseShouldStartStorageRequest(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

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
        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testprojz"
        }
        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("test1", pname)
            assert scope == PropertyScope.Instance
            [instance: test, configuration: [:]]
        }
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        service.frameworkService = fmock.createMock()
        service.pluginService = pmock.createMock()

        assertEquals(0, LogFileStorageRequest.list().size())
        if(null!=clos){
            service.with(clos)
        }
        return service.getLogFileWriterForExecution(e, [:])

    }

    void testRunStorageRequestSuccess(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.storeLogFileSuccess=true
        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeLogFileCalled)
            assertNull(test.storeFiletype)
        }

        assertTrue(test.storeLogFileCalled)
        assertEquals("rdlog", test.storeFiletype)
        assertEquals(testLogFile1.length(),test.storeLength)
        assertEquals(new Date(testLogFile1.lastModified()),test.storeLastModified)

        assertEquals(1,task.count)
        assertTrue(svc.executorService.executeCalled)
        assertEquals(1,svc.getCurrentRequests().size())
    }
    void testRunStorageRequestFailure(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.storeLogFileSuccess=false
        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeLogFileCalled)
            assertNull( test.storeFiletype)
        }

        assertTrue(test.storeLogFileCalled)
        assertEquals("rdlog", test.storeFiletype)
        assertEquals(testLogFile1.length(),test.storeLength)
        assertEquals(new Date(testLogFile1.lastModified()),test.storeLastModified)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0, svc.getCurrentRequests().size())
    }
    void testRunStorageRequestFailureWithRetry(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        grailsApplication.config.rundeck.execution.logs.fileStorage.storageRetryDelay = 30
        grailsApplication.config.rundeck.execution.logs.fileStorage.storageRetryCount = 2


        def test = new testStoragePlugin()
        test.storeLogFileSuccess=false
        LogFileStorageService svc
        boolean queued=false
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            svc.metaClass.queueLogStorageRequest={ Map task, int delay ->
                queued=true
                assertEquals(30,delay)
                assertEquals(testLogFile1,task.file)
            }
            assertFalse(test.storeLogFileCalled)
            assertNull(test.storeFiletype)
        }

        assertTrue(test.storeLogFileCalled)
        assertEquals("rdlog", test.storeFiletype)
        assertEquals(testLogFile1.length(),test.storeLength)
        assertEquals(new Date(testLogFile1.lastModified()),test.storeLastModified)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0, svc.getCurrentRequests().size())
        assertTrue(queued)
    }

    private Map performRunStorage(testStoragePlugin test, String filetype, Execution e, File testfile, Closure clos = null) {
        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testprojz"
        }
        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("test1", pname)
            assert scope == PropertyScope.Instance
            [instance: test, configuration: [:]]
        }
        def emock = new Expando()
        emock.executeCalled=false
        emock.execute={Closure cls->
            emock.executeCalled=true
            assertNotNull(cls)
        }
        service.frameworkService = fmock.createMock()
        service.pluginService = pmock.createMock()
        service.executorService=emock

        assertEquals(0, LogFileStorageRequest.list().size())
        if (null != clos) {
            service.with(clos)
        }
        def task = [id: e.id.toString(), file: testfile, storage: test, filetype: filetype]
        service.runStorageRequest(task)
        return task
    }

    void testRequestLogFileReaderFileDNE(){

        grailsApplication.config.clear()

        def test = null

        def e = createExecution()

        def reader = performReaderRequest(test, false, testLogFileDNE, false, e)
        assertNotNull(reader)
        assertEquals(ExecutionLogState.NOT_FOUND,reader.state)
        assertNull(reader.reader)
    }

    void testRequestLogFileReaderFileDNEWaiting() {

        grailsApplication.config.clear()

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

        grailsApplication.config.clear()

        def test = new testStoragePlugin()
        test.available = false

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

        grailsApplication.config.clear()

        def test = new testStoragePlugin()
        test.available = false

        def reader = performReaderRequest(test, false, testLogFile1, false, createExecution())

        //initialize should not have been called
        assert !test.initializeCalled

        assertNotNull(reader)
        assertEquals(ExecutionLogState.AVAILABLE, reader.state)
        assertNotNull(reader.reader)
        assertTrue(reader.reader instanceof FSStreamingLogReader)
    }


    void testRequestLogFileReaderFileDNEPluginAvailableFalseShouldResultInPendingRemote() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = false

        def reader = performReaderRequest(test, false, testLogFileDNE, false, createExecution())

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.PENDING_REMOTE, reader.state)
        assertNull(reader.reader)
    }

    void testRequestLogFileReaderFileDNEPluginAvailableTrueShouldResultInAvailableRemote() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution())

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.AVAILABLE_REMOTE, reader.state)
        assertNull(reader.reader)
    }

    void testRequestLogFileReaderFileDNEPluginAvailableErrorShouldResultInError() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true
        test.availableException = true

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution())

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionLogState.ERROR, reader.state)
        assertEquals('execution.log.storage.state.ERROR', reader.errorCode)
        assertEquals(['test1','testStoragePlugin.available'], reader.errorData)
        assertNull(reader.reader)
    }
    void testRequestLogFileReaderFileDNEPluginRequestAlreadyPending() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution()){ LogFileStorageService svc->
            svc.metaClass.logFileRetrievalRequestState={Execution execution, String filetype->
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
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true

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

        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testprojz"
        }
        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("test1", pname)
            assert scope == PropertyScope.Instance
            [instance: test, configuration: [:]]
        }
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        service.frameworkService = fmock.createMock()
        service.frameworkService.serverUUID = null
        service.frameworkService.metaClass.isClusterModeEnabled = {
            return isClustered
        }
        service.frameworkService.metaClass.getServerUUID = {
            UUID.randomUUID()
        }
        service.pluginService = pmock.createMock()
        service.metaClass.getFileForExecutionFiletype = { Execution e2, String filetype ->
            assert e == e2
            assert "rdlog"==filetype
            logfile
        }
        if(null!= svcClosure){
            service.with(svcClosure)
        }

        return service.requestLogFileReader(e, LoggingService.LOG_FILE_FILETYPE,performLoad)
    }

    private Execution createExecution(Closure clos=null) {
        def e = new Execution(argString: "-test args", user: "testuser", project: "testprojz", loglevel: 'WARN', doNodedispatch: false)
        if(null!=clos){
            e.with(clos)
        }
        return e
    }

}
