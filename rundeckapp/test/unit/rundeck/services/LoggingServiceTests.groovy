package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.ThreadBoundLogOutputStream
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import grails.test.*
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import rundeck.Execution
import rundeck.services.logging.DisablingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState
import rundeck.services.logging.LoglevelThresholdLogWriter
import rundeck.services.logging.MultiLogWriter

class LoggingServiceTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testLocalFileStorageEnabled() {
        LoggingService svc = new LoggingService()

        assertTrue(svc.isLocalFileStorageEnabled())

        ConfigurationHolder.config=[:]
        ConfigurationHolder.config.rundeck.execution.logs.streamingReaderPlugin= "test"
        assertTrue(svc.isLocalFileStorageEnabled())

        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.localFileStorageEnabled= "false"
        assertTrue(svc.isLocalFileStorageEnabled())

        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.localFileStorageEnabled= "false"
        ConfigurationHolder.config.rundeck.execution.logs.streamingReaderPlugin="test"
        assertFalse(svc.isLocalFileStorageEnabled())
    }
    void testLoggingReaderPluginConfiguration() {
        LoggingService svc = new LoggingService()

        assertNull(svc.getConfiguredStreamingReaderPluginName())

        ConfigurationHolder.config=[:]
        ConfigurationHolder.config.rundeck.execution.logs.streamingReaderPlugin= "test"
        assertEquals("test", svc.getConfiguredStreamingReaderPluginName())
    }
    void testLoggingWriterPluginsConfiguration() {
        LoggingService svc = new LoggingService()

        assertEquals(0,svc.listConfiguredStreamingWriterPluginNames().size())

        ConfigurationHolder.config=[:]
        ConfigurationHolder.config.rundeck.execution.logs.streamingWriterPlugins= "test"
        assertEquals(1, svc.listConfiguredStreamingWriterPluginNames().size())
        assertEquals(["test"], svc.listConfiguredStreamingWriterPluginNames())

        ConfigurationHolder.config=[:]
        ConfigurationHolder.config.rundeck.execution.logs.streamingWriterPlugins= "test,test2"
        assertEquals(2, svc.listConfiguredStreamingWriterPluginNames().size())
        assertEquals(["test","test2"], svc.listConfiguredStreamingWriterPluginNames())
    }

    class testWriter implements StreamingLogWriter{
        String name
        @Override
        void openStream() throws IOException {

        }

        @Override
        void addEvent(LogEvent event) {

        }

        @Override
        void close() {

        }
    }
    class testPluginWriter extends testWriter implements StreamingLogWriterPlugin{

        @Override
        void initialize(Map<String, ? extends Object> context) {

        }
    }

    void testOpenLogWriterWithPlugins(){
        mockDomain(Execution)
        mockLogging(LoggingService)
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.streamingWriterPlugins = "plugin1,plugin2"

        def writer = new testWriter()
        writer.name = "filewritertest1"


        def lfmock = mockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            writer
        }
        lfmock.demand.generateFilepathForExecution(1..1) { Execution e2 ->
            assertEquals(1, e2.id)
            new File("/test/file/path")
        }

        def plugin1 = new testPluginWriter()
        plugin1.name = "plugin1"
        def plugin2 = new testPluginWriter()
        plugin2.name = "plugin2"
        def plugins = [plugin1: plugin1, plugin2: plugin2]

        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService svc, PropertyResolver resolv, PropertyScope scope ->
            assertTrue (pname in ["plugin1","plugin2"])
            assert scope==PropertyScope.Instance
            [instance:plugins[pname],configuration:[:]]
        }

        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver(2..2) { project ->
            assert project=="testproj"
        }
        registerMetaClass(ExecutionService)
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
//        pluginService.configurePlugin(name, streamingLogWriterPluginProviderService,
//                frameworkService.getFrameworkPropertyResolver(execution.project), PropertyScope.Instance)
        LoggingService svc = new LoggingService()
        svc.logFileStorageService = lfmock.createMock()
        svc.pluginService=pmock.createMock()
        svc.frameworkService=fmock.createMock()

        def execwriter = svc.openLogWriter(e, LogLevel.NORMAL, [test:"blah"])
        assertNotNull(execwriter)
        assertEquals(new File("/test/file/path"), execwriter.filepath)
        assertNotNull(execwriter.writer)

        assertTrue(execwriter.writer instanceof LoglevelThresholdLogWriter)
        LoglevelThresholdLogWriter filtered=execwriter.writer
        assertEquals(LogLevel.NORMAL, filtered.threshold)

        assertTrue(filtered.writer instanceof MultiLogWriter)
        MultiLogWriter multi= filtered.writer
        def multiWriters = multi.writers
        assertEquals(3, multiWriters.size())
        assertTrue(multiWriters[0] instanceof DisablingLogWriter)
        assertEquals(plugin1, multiWriters[0].writer)
        assertTrue(multiWriters[1] instanceof DisablingLogWriter)
        assertEquals(plugin2, multiWriters[1].writer)
        assertEquals(writer,multiWriters[2])
    }
    void testOpenLogWriterNoPlugins(){
        mockDomain(Execution)
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = mockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            writer
        }
        lfmock.demand.generateFilepathForExecution(1..1) { Execution e2 ->
            assertEquals(1, e2.id)
            new File("/test/file/path")
        }
        LoggingService svc = new LoggingService()
        svc.logFileStorageService=lfmock.createMock()

        def execwriter = svc.openLogWriter(e, LogLevel.NORMAL, [test:"blah"])
        assertNotNull(execwriter)
        assertEquals(new File("/test/file/path"), execwriter.filepath)
        assertNotNull(execwriter.writer)

        assertTrue(execwriter.writer instanceof LoglevelThresholdLogWriter)
        LoglevelThresholdLogWriter filtered=execwriter.writer
        assertEquals(LogLevel.NORMAL, filtered.threshold)

        assertTrue(filtered.writer instanceof MultiLogWriter)
        MultiLogWriter multi= filtered.writer
        assertEquals(1,multi.writers.size())
        assertEquals([writer],multi.writers)
    }

    class testReader implements StreamingLogReader{
        @Override
        void openStream(Long offset) throws IOException {

        }

        @Override
        long getTotalSize() {
            return 0
        }

        @Override
        Date getLastModified() {
            return null
        }

        @Override
        void close() throws IOException {

        }

        @Override
        boolean isComplete() {
            return false
        }

        @Override
        long getOffset() {
            return 0
        }

        @Override
        boolean hasNext() {
            return false
        }

        @Override
        LogEvent next() {
            return null
        }

        @Override
        void remove() {

        }
    }
    class testReaderPlugin extends testReader implements StreamingLogReaderPlugin{
        boolean canInitialize
        @Override
        boolean initialize(Map<String, ? extends Object> context) {
            return canInitialize
        }
    }
    void testGetLogReaderWithoutPlugin(){
        mockDomain(Execution)
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def test=new ExecutionLogReader()

        LoggingService svc = new LoggingService()
        def lfsvcmock = mockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1){Execution e2->
            assert e==e2
            test
        }
        svc.logFileStorageService=lfsvcmock.createMock()
        def reader = svc.getLogReader(e)
        assertNotNull(reader)
        assertEquals(test,reader)
    }
    void testGetLogReaderWithPluginInitializesTrue(){
        mockDomain(Execution)
        mockLogging(LoggingService)
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.streamingReaderPlugin = "plugin1"
        def test=new testReaderPlugin()
        test.canInitialize=true

        LoggingService svc = new LoggingService()
        assertEquals("plugin1", svc.getConfiguredStreamingReaderPluginName())

        def lfsvcmock = mockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1){Execution e2->
            assert e==e2
            fail("should not be called")
        }
        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("plugin1",pname)
            assert scope == PropertyScope.Instance
            [instance:test,configuration:[:]]
        }
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver(2..2) { project ->
            assert project == "testproj"
        }
        registerMetaClass(ExecutionService)
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        svc.logFileStorageService=lfsvcmock.createMock()
        svc.pluginService=pmock.createMock()
        svc.frameworkService=fmock.createMock()

        def reader = svc.getLogReader(e)
        assertNotNull(reader)
        assertEquals(ExecutionLogState.AVAILABLE, reader.state)
        assertEquals(test,reader.reader)
    }
    void testGetLogReaderWithPluginInitializesFalse(){
        mockDomain(Execution)
        mockLogging(LoggingService)
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        ConfigurationHolder.config = [:]
        ConfigurationHolder.config.rundeck.execution.logs.streamingReaderPlugin = "plugin1"
        def test=new testReaderPlugin()
        test.canInitialize=false

        LoggingService svc = new LoggingService()
        assertEquals("plugin1", svc.getConfiguredStreamingReaderPluginName())

        def lfsvcmock = mockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1){Execution e2->
            assert e==e2
            fail("should not be called")
        }
        def pmock = mockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("plugin1",pname)
            assert scope == PropertyScope.Instance
            [instance: test, configuration: [:]]
        }
        def fmock = mockFor(FrameworkService)
        fmock.demand.getFrameworkPropertyResolver(2..2) { project ->
            assert project == "testproj"
        }
        registerMetaClass(ExecutionService)
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        svc.logFileStorageService=lfsvcmock.createMock()
        svc.pluginService=pmock.createMock()
        svc.frameworkService=fmock.createMock()

        def reader = svc.getLogReader(e)
        assertNotNull(reader)
        assertEquals(ExecutionLogState.WAITING, reader.state)
        assertNull(reader.reader)
    }

    public testCreateLogOutputStream() {
        LoggingService svc = new LoggingService()
        def stream = svc.createLogOutputStream(new testWriter(), LogLevel.NORMAL, null)
        assertNotNull(stream)
        assert stream instanceof ThreadBoundLogOutputStream
    }
}
