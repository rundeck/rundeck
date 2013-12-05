package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.core.logging.KeyedLogFileStorage
import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.logging.LogFileStorageException
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.KeyedLogFileStoragePlugin
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
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.storageRetryDelay = 30

        assertEquals(30, svc.getConfiguredStorageRetryDelay())
        assertEquals(1, svc.getConfiguredStorageRetryCount())

        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.storageRetryDelay = 30
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.storageRetryCount = 10

        assertEquals(30, svc.getConfiguredStorageRetryDelay())
        assertEquals(10, svc.getConfiguredStorageRetryCount())
    }
    void testConfiguredRetrievalRetry() {
        LogFileStorageService svc = new LogFileStorageService()

        ConfigurationHolder.config = [:]
        assertEquals(60,svc.getConfiguredRetrievalRetryDelay())
        assertEquals(3,svc.getConfiguredRetrievalRetryCount())


        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30

        assertEquals(30, svc.getConfiguredRetrievalRetryDelay())
        assertEquals(3, svc.getConfiguredRetrievalRetryCount())

        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retrievalRetryCount = 10

        assertEquals(30, svc.getConfiguredRetrievalRetryDelay())
        assertEquals(10, svc.getConfiguredRetrievalRetryCount())
    }
    void testIsCachedItemFresh() {
        LogFileStorageService svc = new LogFileStorageService()

        ConfigurationHolder.config = [:]
        assertTrue(svc.isResultCacheItemFresh([time: new Date(), count: 0]))
        assertTrue(svc.isResultCacheItemAllowedRetry([time: new Date(), count: 0]))

        assertFalse(svc.isResultCacheItemFresh([time: new Date(System.currentTimeMillis()- (61*1000)), count: 0]))
        assertFalse(svc.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))


        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30

        assertTrue(svc.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0]))
        assertFalse(svc.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0]))
        assertFalse(svc.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))


        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retrievalRetryDelay = 30
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.retrievalRetryCount = 10

        assertTrue(svc.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0]))
        assertFalse(svc.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0]))
        assertTrue(svc.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))
        assertFalse(svc.isResultCacheItemAllowedRetry([time: new Date(), count: 10]))
    }
    void testGetFileForKey(){
        mockLogging(LogFileStorageService)
        LogFileStorageService svc = new LogFileStorageService()
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        svc.frameworkService = fmock.createMock()
        def result = svc.getFileForLocalPath("abc")
        assertNotNull(result)
        assertEquals(new File("/tmp/logs/rundeck/abc"),result)
    }
    void testGetFileForKeyNotFound(){
        mockLogging(LogFileStorageService)
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever([:])
        }
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService = fmock.createMock()
        try {
            def result = svc.getFileForLocalPath("abc")
            fail("Expected exception")
        } catch (IllegalStateException e) {
            assertEquals("framework.logs.dir is not set in framework.properties", e.message)
        }
    }
    void testCacheResult(){
        mockLogging(LogFileStorageService)
        LogFileStorageService svc = new LogFileStorageService()
        ConfigurationHolder.config=[:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        def result = svc.cacheRetrievalState("1", LogFileState.NOT_FOUND, 0, 'error','errorCode',['errorData'])
        assertNotNull(result)
        assertEquals("1",result.id)
        assertEquals(0, result.count)
        assertEquals('errorCode',result.errorCode)
        assertEquals(['errorData'],result.errorData)
        assertEquals('error',result.error)
        assertEquals('test1',result.name)
        assertEquals(LogFileState.NOT_FOUND,result.state)
        assertEquals(1, svc.getCurrentRetrievalResults().size())

        Map result1 = svc.getRetrievalCacheResult("1")
        assertNotNull(result1)
    }
    void testCacheResultDefaults(){
        mockLogging(LogFileStorageService)
        LogFileStorageService svc = new LogFileStorageService()
        ConfigurationHolder.config=[:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        def result = svc.cacheRetrievalState("1", LogFileState.NOT_FOUND, 0, 'error', null, null)
        assertNotNull(result)
        assertEquals("1",result.id)
        assertEquals(0, result.count)
        assertEquals('execution.log.storage.retrieval.ERROR',result.errorCode)
        assertEquals(['test1','error'],result.errorData)
        assertEquals('error',result.error)
        assertEquals('test1',result.name)
        assertEquals(LogFileState.NOT_FOUND,result.state)
        assertEquals(1, svc.getCurrentRetrievalResults().size())

        Map result1 = svc.getRetrievalCacheResult("1")
        assertNotNull(result1)
    }
    void testgetLogFileWriterWithoutPlugin(){
        mockDomain(Execution)
        mockLogging(LogFileStorageService)
        ConfigurationHolder.config = [:]
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testproj"
        }
        registerMetaClass(ExecutionService)
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService=fmock.createMock()

        def writer = svc.getLogFileWriterForExecution(e, [:])
        assertNotNull(writer)
        assert writer instanceof EventStreamingLogWriter
    }
    class testStoragePlugin implements KeyedLogFileStoragePlugin{
        Map<String, ? extends Object> context
        boolean available
        boolean availableException
        String availableFilekey
        boolean initializeCalled
        boolean storeLogFileCalled
        boolean storeLogFileSuccess
        String storeFilekey
        boolean retrieveLogFileCalled
        boolean retrieveLogFileSuccess
        String retrieveFilekey
        long storeLength
        Date storeLastModified

        @Override
        void initialize(Map<String, ? extends Object> context) {
            initializeCalled=true
            this.context=context;
        }

        @Override
        boolean isAvailable(String key) throws LogFileStorageException {
            availableFilekey=key
            return isAvailable()
        }

        @Override
        boolean isAvailable() throws LogFileStorageException {
            if(availableException){
                throw new LogFileStorageException("testStoragePlugin.available")
            }
            return available
        }

        @Override
        boolean store(String key, InputStream stream, long length, Date lastModified) throws IOException, LogFileStorageException {
            storeFilekey=key
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
        boolean retrieve(String key, OutputStream stream) throws IOException, LogFileStorageException {
            retrieveFilekey=key
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
        registerMetaClass(ExecutionService)
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService = fmock.createMock()
        svc.pluginService = pmock.createMock()

        assertEquals(0, LogFileStorageRequest.list().size())
        if(null!=clos){
            svc.with(clos)
        }
        return svc.getLogFileWriterForExecution(e, [:])

    }

    void testRunStorageRequestSuccess(){
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.storeLogFileSuccess=true
        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeLogFileCalled)
            assertNull(test.storeFilekey)
        }

        assertTrue(test.storeLogFileCalled)
        assertEquals("rdlog", test.storeFilekey)
        assertEquals(testLogFile1.length(),test.storeLength)
        assertEquals(new Date(testLogFile1.lastModified()),test.storeLastModified)

        assertEquals(1,task.count)
        assertTrue(svc.executorService.executeCalled)
        assertEquals(1,svc.getCurrentRequests().size())
    }
    void testRunStorageRequestFailure(){
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.storeLogFileSuccess=false
        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeLogFileCalled)
            assertNull( test.storeFilekey)
        }

        assertTrue(test.storeLogFileCalled)
        assertEquals("rdlog", test.storeFilekey)
        assertEquals(testLogFile1.length(),test.storeLength)
        assertEquals(new Date(testLogFile1.lastModified()),test.storeLastModified)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0, svc.getCurrentRequests().size())
    }
    void testRunStorageRequestFailureWithRetry(){
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.storageRetryDelay = 30
        ConfigurationHolder.config.rundeck.execution.logs.fileStorage.storageRetryCount = 2


        def test = new testStoragePlugin()
        test.storeLogFileSuccess=false
        registerMetaClass(LogFileStorageService)
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
            assertNull(test.storeFilekey)
        }

        assertTrue(test.storeLogFileCalled)
        assertEquals("rdlog", test.storeFilekey)
        assertEquals(testLogFile1.length(),test.storeLength)
        assertEquals(new Date(testLogFile1.lastModified()),test.storeLastModified)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0, svc.getCurrentRequests().size())
        assertTrue(queued)
    }

    private Map performRunStorage(testStoragePlugin test, String filekey, Execution e, File testfile, Closure clos = null) {
        mockDomain(Execution)
        mockDomain(LogFileStorageRequest)
        mockLogging(LogFileStorageService)
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
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService = fmock.createMock()
        svc.pluginService = pmock.createMock()
        svc.executorService=emock

        assertEquals(0, LogFileStorageRequest.list().size())
        if (null != clos) {
            svc.with(clos)
        }
        def task = [id: e.id.toString(), file: testfile, storage: test, key: filekey]
        svc.runStorageRequest(task)
        return task
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

        ConfigurationHolder.config = [:]

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
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

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
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

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
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

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
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution()){ LogFileStorageService svc->
            svc.metaClass.logFileRetrievalRequestState={Execution execution, String filekey->
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
        mockDomain(Execution)
        mockDomain(LogFileStorageRequest)
        mockLogging(LogFileStorageService)

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
        registerMetaClass(ExecutionService)
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        LogFileStorageService svc = new LogFileStorageService()
        svc.frameworkService = fmock.createMock()
        svc.frameworkService.serverUUID = null
        svc.frameworkService.metaClass.isClusterModeEnabled = {
            return isClustered
        }
        svc.frameworkService.metaClass.getServerUUID = {
            UUID.randomUUID()
        }
        svc.pluginService = pmock.createMock()
        svc.metaClass.getFileForExecutionFilekey = { Execution e2, String filekey ->
            assert e == e2
            assert "rdlog"==filekey
            logfile
        }
        if(null!= svcClosure){
            svc.with(svcClosure)
        }

        return svc.requestLogFileReader(e, LoggingService.LOG_FILE_STORAGE_KEY,performLoad)
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
