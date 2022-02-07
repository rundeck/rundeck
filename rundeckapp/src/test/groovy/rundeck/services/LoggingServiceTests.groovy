/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

package rundeck.services

import com.dtolabs.rundeck.core.utils.ThreadBoundLogOutputStream
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import groovy.mock.interceptor.MockFor

import static org.junit.Assert.*

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import grails.test.runtime.DirtiesRuntime
import grails.web.mapping.LinkGenerator
import rundeck.Execution
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.services.logging.DisablingLogWriter
import rundeck.services.logging.ExecutionLogReader
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.LineCountingLogWriter
import rundeck.services.logging.LoggingThreshold
import rundeck.services.logging.LoglevelThresholdLogWriter
import rundeck.services.logging.MultiLogWriter
import rundeck.services.logging.NodeCountingLogWriter
import rundeck.services.logging.StepLabellingStreamingLogWriter
import rundeck.services.logging.ThresholdLogWriter

class LoggingServiceTests  extends HibernateSpec implements ServiceUnitTest<LoggingService> {

    List<Class> getDomainClasses() { [Execution, LogFileStorageService, Workflow, CommandExec] }

    //TODO: cleanup test code

    void testLocalFileStorageEnabled() {
        when:
        LoggingService svc = new LoggingService()
        svc.grailsApplication=grailsApplication

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(2..2) { String key ->
            if(key == "execution.logs.localFileStorageEnabled"){
                return null
            }
            if(key == "execution.logs.streamingReaderPlugin"){
                return null
            }
            return ""
        }
        ///
        csmock.demand.getString(3..3) { String key ->
            if(key == "execution.logs.localFileStorageEnabled"){
                return null
            }
            if(key == "execution.logs.streamingReaderPlugin"){
                return "test"
            }
            return ""
        }
        ///
        csmock.demand.getString(2..2) { String key ->
            if(key == "execution.logs.localFileStorageEnabled"){
                return "false"
            }
            if(key == "execution.logs.streamingReaderPlugin"){
                return null
            }
            return ""
        }

        //
        csmock.demand.getString(3..3) { String key ->
            if(key == "execution.logs.localFileStorageEnabled"){
                return "false"
            }
            if(key == "execution.logs.streamingReaderPlugin"){
                return "test"
            }
            return ""
        }
        svc.configurationService=csmock.proxyInstance()

        assertTrue(svc.isLocalFileStorageEnabled())

        assertTrue(svc.isLocalFileStorageEnabled())

        assertTrue(svc.isLocalFileStorageEnabled())

        then:
        assertFalse(svc.isLocalFileStorageEnabled())
    }
    void testLoggingReaderPluginConfiguration() {
        when:
        grailsApplication.config.clear()
        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(1..1) { x ->
            ""
        }
        csmock.demand.getString(4..4) { x ->
            "test"
        }
        service.configurationService=csmock.proxyInstance()
        assertNull(service.getConfiguredStreamingReaderPluginName())
        then:
        assertEquals("test", service.getConfiguredStreamingReaderPluginName())
    }
    void testLoggingWriterPluginsConfiguration() {
        when:
        grailsApplication.config.clear()

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(1..1) { x ->
            null
        }
        csmock.demand.getString(4..4) { x ->
            "test"
        }
        csmock.demand.getString(4..4) { x ->
            "test,test2"
        }
        service.configurationService=csmock.proxyInstance()

        assertEquals(0,service.listConfiguredStreamingWriterPluginNames().size())
        assertEquals(1, service.listConfiguredStreamingWriterPluginNames().size())
        assertEquals(["test"], service.listConfiguredStreamingWriterPluginNames())

        then:
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
        Map<String, ? extends Object> context
        @Override
        void initialize(Map<String, ? extends Object> context) {
            this.context = context
        }
    }

    @DirtiesRuntime
    void testOpenLogWriterWithPlugins(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        def writer = new testWriter()
        writer.name = "filewritertest1"


        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
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

        def pmock = new MockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService svc, PropertyResolver resolv, PropertyScope scope ->
            assertTrue (pname in ["plugin1","plugin2"])
            assert scope==PropertyScope.Instance
            [instance:plugins[pname],configuration:[:]]
        }

        def fmock = new MockFor(FrameworkService)
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
        service.logFileStorageService = lfmock.proxyInstance()
        service.pluginService=pmock.proxyInstance()
        service.frameworkService=fmock.proxyInstance()

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(2..2) { x ->
            "plugin1,plugin2"
        }
        csmock.demand.getBoolean(1..1) { String value, boolean defVal->
            true
        }
        csmock.demand.getString(2..2) { x ->
            null
        }
        service.configurationService=csmock.proxyInstance()

        def execwriter = service.openLogWriter(e, LogLevel.NORMAL, [test:"blah"])
        assertNotNull(execwriter)
        assertEquals(new File("/test/file/path"), execwriter.filepath)
        assertNotNull(execwriter.writer)

        assertTrue(execwriter.writer instanceof LoglevelThresholdLogWriter)
        LoglevelThresholdLogWriter filtered=execwriter.writer
        assertEquals(LogLevel.NORMAL, filtered.threshold)

        assertTrue(filtered.writer instanceof MultiLogWriter)
        MultiLogWriter multi = filtered.writer
        def multiWriters = multi.writers
        then:
        assertEquals(3, multiWriters.size())
        assertTrue(multiWriters[0] instanceof DisablingLogWriter)
        assertTrue(multiWriters[0].writer instanceof StepLabellingStreamingLogWriter)
        assertEquals(plugin1, multiWriters[0].writer.writer)
        assertTrue(multiWriters[1] instanceof DisablingLogWriter)
        assertTrue(multiWriters[1].writer instanceof StepLabellingStreamingLogWriter)
        assertEquals(plugin2, multiWriters[1].writer.writer)
        assertTrue(multiWriters[2] instanceof DisablingLogWriter)
        assertEquals(writer, multiWriters[2].writer)
    }
    @DirtiesRuntime
    void testOpenLogWriterWithPlugins_stepLabelsDisabled(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(2..2) { String value ->
            "plugin1,plugin2"
        }
        csmock.demand.getBoolean(1..1) { String value, boolean defVal ->
            false
        }
        csmock.demand.getString(2..2) { String value ->
            null
        }

        service.configurationService=csmock.proxyInstance()
        def writer = new testWriter()
        writer.name = "filewritertest1"


        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(e.id, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(e.id, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }

        def plugin1 = new testPluginWriter()
        plugin1.name = "plugin1"
        def plugin2 = new testPluginWriter()
        plugin2.name = "plugin2"
        def plugins = [plugin1: plugin1, plugin2: plugin2]

        def pmock = new MockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService svc, PropertyResolver resolv, PropertyScope scope ->
            assertTrue (pname in ["plugin1","plugin2"])
            assert scope==PropertyScope.Instance
            [instance:plugins[pname],configuration:[:]]
        }

        def fmock = new MockFor(FrameworkService)
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
        service.logFileStorageService = lfmock.proxyInstance()
        service.pluginService=pmock.proxyInstance()
        service.frameworkService=fmock.proxyInstance()

        def execwriter = service.openLogWriter(e, LogLevel.NORMAL, [test:"blah"])
        assertNotNull(execwriter)
        assertEquals(new File("/test/file/path"), execwriter.filepath)
        assertNotNull(execwriter.writer)

        assertTrue(execwriter.writer instanceof LoglevelThresholdLogWriter)
        LoglevelThresholdLogWriter filtered=execwriter.writer
        assertEquals(LogLevel.NORMAL, filtered.threshold)

        assertTrue(filtered.writer instanceof MultiLogWriter)
        MultiLogWriter multi = filtered.writer
        def multiWriters = multi.writers
        then:
        assertEquals(3, multiWriters.size())
        assertTrue(multiWriters[0] instanceof DisablingLogWriter)
        assertFalse(multiWriters[0].writer instanceof StepLabellingStreamingLogWriter)
        assertEquals(plugin1, multiWriters[0].writer)
        assertTrue(multiWriters[1] instanceof DisablingLogWriter)
        assertFalse(multiWriters[1].writer instanceof StepLabellingStreamingLogWriter)
        assertEquals(plugin2, multiWriters[1].writer)
        assertTrue(multiWriters[2] instanceof DisablingLogWriter)
        assertEquals(writer, multiWriters[2].writer)
    }

    void testOpenLogWriterNoPlugins(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(e.id, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertEquals(null,x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(e.id, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(1..1) { x ->
            ""
        }
        service.configurationService=csmock.proxyInstance()

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
        then:
        assertEquals(1,multi.writers.size())
        assertTrue(multi.writers[0] instanceof DisablingLogWriter)
        assertEquals(writer, multi.writers[0].writer)
    }
    void testOpenLogWriter_with_fileSizeThresholdWatcher(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(e.id, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNotNull("expected a value watcher for logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(e.id, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(1..1) { x ->
            ""
        }
        service.configurationService=csmock.proxyInstance()

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
        then:
        assertNotNull(unwrapped.writer)
        assertTrue(unwrapped.writer instanceof LoglevelThresholdLogWriter)
    }
    void testOpenLogWriter_with_maxLinesThresholdWatcher(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(e.id, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNull("expected no logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(e.id, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(1..1) { x ->
            ""
        }
        service.configurationService=csmock.proxyInstance()

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
        then:
        assertNotNull(unwrapped.writer)
        assertTrue(unwrapped.writer instanceof LoglevelThresholdLogWriter)
    }
    void testOpenLogWriter_with_nodeLinesThresholdWatcher(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(e.id, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNull("expected no logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(e.id, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(1..1) { x ->
            ""
        }
        service.configurationService=csmock.proxyInstance()


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
        then:
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
        when:
        grailsApplication.config.clear()
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def test=new ExecutionLogReader()

        def lfsvcmock = new MockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1){Execution e2, String key->
            assert e==e2
            assert key=='rdlog'
            test
        }
        service.logFileStorageService=lfsvcmock.proxyInstance()

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(1..1) { x ->
            ""
        }
        service.configurationService=csmock.proxyInstance()

        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        def reader = service.getLogReader(e)
        then:
        assertNotNull(reader)
        assertEquals(test,reader)
    }
    @DirtiesRuntime
    void testGetLogReaderWithPluginInitializesTrue(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingReaderPlugin = "plugin1"
        def test=new testReaderPlugin()
        test.canInitialize=true

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(4..4) { x ->
            "plugin1"
        }
        service.configurationService=csmock.proxyInstance()


        assertEquals("plugin1", service.getConfiguredStreamingReaderPluginName())

        def lfsvcmock = new MockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1) { Execution e2, String key ->
            assert e == e2
            assert key == 'rdlog'
            fail("should not be called")
        }
        def pmock = new MockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("plugin1",pname)
            assert scope == PropertyScope.Instance
            [instance:test,configuration:[:]]
        }
        def fmock = new MockFor(FrameworkService)
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

        service.logFileStorageService=lfsvcmock.proxyInstance()
        service.pluginService=pmock.proxyInstance()
        service.frameworkService=fmock.proxyInstance()

        def reader = service.getLogReader(e)
        then:
        assertNotNull(reader)
        assertEquals(ExecutionFileState.AVAILABLE, reader.state)
        assertEquals(test,reader.reader)
    }
    @DirtiesRuntime
    void testGetLogReaderWithPluginInitializesFalse(){
        when:
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingReaderPlugin = "plugin1"
        def test=new testReaderPlugin()
        test.canInitialize=false

        def csmock = new MockFor(ConfigurationService)
        csmock.demand.getString(4..4) { String value ->
            "plugin1"
        }
        service.configurationService=csmock.proxyInstance()

        assertEquals("plugin1", service.getConfiguredStreamingReaderPluginName())

        def lfsvcmock = new MockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1) { Execution e2, String key ->
            assert e == e2
            assert key == 'rdlog'
            fail("should not be called")
        }
        def pmock = new MockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("plugin1",pname)
            assert scope == PropertyScope.Instance
            [instance: test, configuration: [:]]
        }
        def fmock = new MockFor(FrameworkService)
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

        service.logFileStorageService=lfsvcmock.proxyInstance()
        service.pluginService=pmock.proxyInstance()
        service.frameworkService=fmock.proxyInstance()

        def reader = service.getLogReader(e)
        then:
        assertNotNull(reader)
        assertEquals(ExecutionFileState.WAITING, reader.state)
        assertNull(reader.reader)
    }

    public testCreateLogOutputStream() {
        when:
        LoggingService svc = new LoggingService()
        def stream = svc.createLogOutputStream(new testWriter(), LogLevel.NORMAL, null, null)
        then:
        assertNotNull(stream)
        assert stream instanceof ThreadBoundLogOutputStream
    }
}
