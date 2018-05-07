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

import groovy.mock.interceptor.MockFor

import static org.junit.Assert.*

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
import grails.web.mapping.LinkGenerator
import rundeck.Execution
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.services.logging.DisablingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState
import rundeck.services.logging.LineCountingLogWriter
import rundeck.services.logging.LoggingThreshold
import rundeck.services.logging.LoglevelThresholdLogWriter
import rundeck.services.logging.MultiLogWriter
import rundeck.services.logging.NodeCountingLogWriter
import rundeck.services.logging.StepLabellingStreamingLogWriter
import rundeck.services.logging.ThresholdLogWriter

@TestFor(LoggingService)
@Mock([Execution, LogFileStorageService, Workflow, CommandExec])
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
        Map<String, ? extends Object> context
        @Override
        void initialize(Map<String, ? extends Object> context) {
            this.context = context
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
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.streamingWriterPlugins = "plugin1,plugin2"
        grailsApplication.config.rundeck.execution.logs.plugins.streamingWriterStepLabelsEnabled = "false"

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
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertEquals(null,x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()
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
        assertTrue(multi.writers[0] instanceof DisablingLogWriter)
        assertEquals(writer, multi.writers[0].writer)
    }
    void testOpenLogWriter_with_fileSizeThresholdWatcher(){
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())

        grailsApplication.config.clear()
        def writer = new testWriter()
        writer.name = "filewritertest1"
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNotNull("expected a value watcher for logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()
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
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNull("expected no logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()
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
        def lfmock = new MockFor(LogFileStorageService)
        lfmock.demand.getLogFileWriterForExecution(1..1) { Execution e2, defaultMeta, x ->
            assertEquals(1, e2.id)
            assertEquals([test: "blah"], defaultMeta)
            assertNull("expected no logging file size threshold",x)
            writer
        }
        lfmock.demand.getFileForExecutionFiletype(1..1) {
            Execution e2, String filetype, boolean stored, boolean partial ->
            assertEquals(1, e2.id)
            assertEquals("rdlog", filetype)
            assertEquals(false, stored)
            new File("/test/file/path")
        }
        service.logFileStorageService=lfmock.proxyInstance()
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

        def lfsvcmock = new MockFor(LogFileStorageService)
        lfsvcmock.demand.requestLogFileReader(1..1){Execution e2, String key->
            assert e==e2
            assert key=='rdlog'
            test
        }
        service.logFileStorageService=lfsvcmock.proxyInstance()
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
