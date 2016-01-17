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
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.runtime.DirtiesRuntime
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import rundeck.Execution
import rundeck.services.logging.DisablingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState
import rundeck.services.logging.LineCountingLogWriter
import rundeck.services.logging.LoggingThreshold
import rundeck.services.logging.LoglevelThresholdLogWriter
import rundeck.services.logging.MultiLogWriter
import rundeck.services.logging.NodeCountingLogWriter
import rundeck.services.logging.ThresholdLogWriter

@TestFor(LoggingService)
@Mock([Execution,LogFileStorageService])
class LoggingServiceTests  {

    void testLocalFileStorageEnabled() {
        LoggingService svc = new LoggingService()
        svc.grailsApplication=grailsApplication

        assertTrue(svc.isLocalFileStorageEnabled())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingReaderPlugin= "test"
        assertTrue(svc.isLocalFileStorageEnabled())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.localFileStorageEnabled= "false"
        assertTrue(svc.isLocalFileStorageEnabled())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.localFileStorageEnabled= "false"
        grailsApplication.config.rundeck.execution.logs.streamingReaderPlugin="test"
        assertFalse(svc.isLocalFileStorageEnabled())
    }
    void testLoggingReaderPluginConfiguration() {
        grailsApplication.config.clear()
        assertNull(service.getConfiguredStreamingReaderPluginName())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingReaderPlugin= "test"
        assertEquals("test", service.getConfiguredStreamingReaderPluginName())
    }
    void testLoggingWriterPluginsConfiguration() {
        grailsApplication.config.clear()
        assertEquals(0,service.listConfiguredStreamingWriterPluginNames().size())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingWriterPlugins= "test"
        assertEquals(1, service.listConfiguredStreamingWriterPluginNames().size())
        assertEquals(["test"], service.listConfiguredStreamingWriterPluginNames())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingWriterPlugins= "test,test2"
        assertEquals(2, service.listConfiguredStreamingWriterPluginNames().size())
        assertEquals(["test","test2"], service.listConfiguredStreamingWriterPluginNames())
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

    @DirtiesRuntime
    void testOpenLogWriterWithPlugins(){
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingWriterPlugins = "plugin1,plugin2"

        def writer = new testWriter()
        writer.name = "filewritertest1"


        def lfmock = mockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) { Execution e2, String filetype, boolean stored ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
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

        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        ExecutionService.metaClass.static.generateServerURL = { LinkGenerator grailsLinkGenerator ->
            ''
        }
        ExecutionService.metaClass.static.generateExecutionURL= { Execution execution, LinkGenerator grailsLinkGenerator ->
            ''
        }
//        pluginService.configurePlugin(name, streamingLogWriterPluginProviderService,
//                frameworkService.getFrameworkPropertyResolver(execution.project), PropertyScope.Instance)
        service.logFileStorageService = lfmock.createMock()
        service.pluginService=pmock.createMock()
        service.frameworkService=fmock.createMock()

        def execwriter = service.openLogWriter(e, LogLevel.NORMAL, [test:"blah"])
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
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = mockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertEquals(null,x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) { Execution e2, String filetype, boolean stored ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.createMock()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        def execwriter = service.openLogWriter(e, LogLevel.NORMAL, [test:"blah"])
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
    void testOpenLogWriter_with_fileSizeThresholdWatcher(){
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = mockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNotNull("expected a value watcher for logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) { Execution e2, String filetype, boolean stored ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.createMock()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        def t = new LoggingThreshold()
        t.type=LoggingThreshold.TOTAL_FILE_SIZE
        def execwriter = service.openLogWriter(e, LogLevel.NORMAL, [test:"blah"],t)
        assertNotNull(execwriter)
        assertEquals(new File("/test/file/path"), execwriter.filepath)
        assertNotNull(execwriter.writer)

        assertTrue(execwriter.writer instanceof ThresholdLogWriter)
        ThresholdLogWriter unwrapped=execwriter.writer
        assertNotNull(unwrapped.writer)
        assertTrue(unwrapped.writer instanceof LoglevelThresholdLogWriter)
    }
    void testOpenLogWriter_with_maxLinesThresholdWatcher(){
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = mockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNull("expected no logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) { Execution e2, String filetype, boolean stored ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.createMock()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        def t = new LoggingThreshold()
        t.type=LoggingThreshold.TOTAL_LINES

        def execwriter = service.openLogWriter(e, LogLevel.NORMAL, [test:"blah"],t)

        assertNotNull(t.valueHolder)
        assertTrue(t.valueHolder instanceof LineCountingLogWriter)

        assertNotNull(execwriter)
        assertEquals(new File("/test/file/path"), execwriter.filepath)
        assertNotNull(execwriter.writer)

        assertTrue(execwriter.writer instanceof ThresholdLogWriter)
        ThresholdLogWriter unwrapped=execwriter.writer
        assertNotNull(unwrapped.writer)
        assertTrue(unwrapped.writer instanceof LoglevelThresholdLogWriter)
    }
    void testOpenLogWriter_with_nodeLinesThresholdWatcher(){
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = mockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNull("expected no logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) { Execution e2, String filetype, boolean stored ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.createMock()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        def t = new LoggingThreshold()
        t.type=LoggingThreshold.LINES_PER_NODE

        def execwriter = service.openLogWriter(e, LogLevel.NORMAL, [test:"blah"],t)

        assertNotNull(t.valueHolder)
        assertTrue(t.valueHolder instanceof NodeCountingLogWriter)

        assertNotNull(execwriter)
        assertEquals(new File("/test/file/path"), execwriter.filepath)
        assertNotNull(execwriter.writer)

        assertTrue(execwriter.writer instanceof ThresholdLogWriter)
        ThresholdLogWriter unwrapped=execwriter.writer
        assertNotNull(unwrapped.writer)
        assertTrue(unwrapped.writer instanceof LoglevelThresholdLogWriter)
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
        grailsApplication.config.clear()
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def test=new ExecutionLogReader()

        def lfsvcmock = mockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1){Execution e2, String key->
            assert e==e2
            assert key=='rdlog'
            test
        }
        service.logFileStorageService=lfsvcmock.createMock()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        def reader = service.getLogReader(e)
        assertNotNull(reader)
        assertEquals(test,reader)
    }
    @DirtiesRuntime
    void testGetLogReaderWithPluginInitializesTrue(){
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingReaderPlugin = "plugin1"
        def test=new testReaderPlugin()
        test.canInitialize=true

        assertEquals("plugin1", service.getConfiguredStreamingReaderPluginName())

        def lfsvcmock = mockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1) { Execution e2, String key ->
            assert e == e2
            assert key == 'rdlog'
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
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        ExecutionService.metaClass.static.generateServerURL = { LinkGenerator grailsLinkGenerator ->
            ''
        }
        ExecutionService.metaClass.static.generateExecutionURL= { Execution execution, LinkGenerator grailsLinkGenerator ->
            ''
        }

        service.logFileStorageService=lfsvcmock.createMock()
        service.pluginService=pmock.createMock()
        service.frameworkService=fmock.createMock()

        def reader = service.getLogReader(e)
        assertNotNull(reader)
        assertEquals(ExecutionLogState.AVAILABLE, reader.state)
        assertEquals(test,reader.reader)
    }
    @DirtiesRuntime
    void testGetLogReaderWithPluginInitializesFalse(){
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingReaderPlugin = "plugin1"
        def test=new testReaderPlugin()
        test.canInitialize=false

        assertEquals("plugin1", service.getConfiguredStreamingReaderPluginName())

        def lfsvcmock = mockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1) { Execution e2, String key ->
            assert e == e2
            assert key == 'rdlog'
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
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        ExecutionService.metaClass.static.generateServerURL = { LinkGenerator grailsLinkGenerator ->
            ''
        }
        ExecutionService.metaClass.static.generateExecutionURL= { Execution execution, LinkGenerator grailsLinkGenerator ->
            ''
        }

        service.logFileStorageService=lfsvcmock.createMock()
        service.pluginService=pmock.createMock()
        service.frameworkService=fmock.createMock()

        def reader = service.getLogReader(e)
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
