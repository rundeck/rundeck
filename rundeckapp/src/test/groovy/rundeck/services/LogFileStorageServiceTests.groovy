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

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import grails.test.hibernate.HibernateSpec
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import org.junit.Ignore
import org.rundeck.app.data.providers.logstorage.GormLogFileStorageRequestProvider
import org.springframework.core.task.TaskExecutor
import spock.lang.Specification

import static org.junit.Assert.*

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageOptions
import com.dtolabs.rundeck.core.logging.ExecutionMultiFileStorage
import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException
import com.dtolabs.rundeck.core.logging.MultiFileStorageRequest
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.runtime.DirtiesRuntime
import grails.web.mapping.LinkGenerator

//import org.grails.web.mapping.LinkGenerator
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.TaskScheduler
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.ScheduledExecution
import rundeck.Workflow
import org.rundeck.app.services.ExecutionFile

import org.rundeck.app.services.ExecutionFileProducer
import rundeck.services.logging.ExecutionLogReader
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.LoggingThreshold
import rundeck.services.logging.ProducedExecutionFile

class LogFileStorageServiceTests extends Specification implements DataTest, ServiceUnitTest<LogFileStorageService>  {
    File testLogFile1
    File testLogFileDNE

    //List<Class> getDomainClasses() { [LogFileStorageRequest,Execution,ScheduledExecution,Workflow]}

    /**
     * utility method to mock a class
     */
    private mockWith(Class clazz, Closure clos) {
        def mock = new MockFor(clazz)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }

    def setupSpec(){
        mockDomain LogFileStorageRequest
        mockDomain Execution
        mockDomain ScheduledExecution
        mockDomain Workflow
    }

    def setup() {

        testLogFile1 = File.createTempFile("LogFileStorageServiceTests", ".txt")
        testLogFile1.deleteOnExit()
        testLogFileDNE = File.createTempFile("LogFileStorageServiceTests", ".txt")
        testLogFileDNE.delete()

        service.logFileStorageRequestProvider = new GormLogFileStorageRequestProvider()
    }

    void cleanup() {
        if(null!=testLogFile1 && testLogFile1.exists()){
            testLogFile1.delete()
        }
    }

    @org.junit.Before
    void before() {
        LogFileStorageRequest.metaClass.static.withNewSession = {Closure c -> c.call() }
    }

    void testConfiguredPluginName() {
        assertNull(service.getConfiguredPluginName())
        service.configurationService=mockWith(ConfigurationService){
            getString{String prop,String defval->'test1'}
        }
        assertEquals("test1", service.getConfiguredPluginName())

        expect:
        // asserts validate test
        1 == 1
    }
    void testConfiguredStorageRetryCount() {

        service.configurationService=mockWith(ConfigurationService){
            getInteger{String x, int defval->defval}
        }
        assertEquals(1, service.getConfiguredStorageRetryCount())

        expect:
        // asserts validate test
        1 == 1

    }
    void testConfiguredStorageRetryDelay() {

        service.configurationService=mockWith(ConfigurationService){
            getInteger{String x, int defval->defval}
        }
        assertEquals(60,service.getConfiguredStorageRetryDelay())
        expect:
        // asserts validate test
        1 == 1
    }
    void testConfiguredRetrievalCount() {

        service.configurationService=mockWith(ConfigurationService){
            getInteger{String x, int defval->defval}
        }
        assertEquals(3,service.getConfiguredRetrievalRetryCount())
        expect:
        // asserts validate test
        1 == 1

    }
    void testConfiguredRetrievalDelay() {
        service.configurationService=mockWith(ConfigurationService){
            getInteger{String x, int defval->defval}
        }
        assertEquals(60,service.getConfiguredRetrievalRetryDelay())
        expect:
        // asserts validate test
        1 == 1
    }
    void testIsCachedItemFresh() {


        service.configurationService=mockWith(ConfigurationService){
            getInteger(4..4){String prop,int defval->defval}
        }
        assertTrue(service.isResultCacheItemFresh([time: new Date(), count: 0]))
        assertTrue(service.isResultCacheItemAllowedRetry([time: new Date(), count: 0]))

        assertFalse(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis()- (61*1000)), count: 0]))
        assertFalse(service.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))


        def vals=[:]
        service.configurationService=mockWith(ConfigurationService){
            getInteger(3..3){String prop,int defval->vals[prop]?:defval}
        }
        vals['execution.logs.fileStorage.retrievalRetryDelay'] = 30

        assertTrue(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0]))
        assertFalse(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0]))
        assertFalse(service.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))


        service.configurationService=mockWith(ConfigurationService){
            getInteger(4..4){String prop,int defval->vals[prop]?:defval}
        }
        vals['execution.logs.fileStorage.retrievalRetryDelay'] = 30
        vals['execution.logs.fileStorage.retrievalRetryCount'] = 10

        assertTrue(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0]))
        assertFalse(service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0]))
        assertTrue(service.isResultCacheItemAllowedRetry([time: new Date(), count: 3]))
        assertFalse(service.isResultCacheItemAllowedRetry([time: new Date(), count: 10]))
        expect:
        // asserts validate test
        1 == 1
    }
    void testGetFileForLocalPath(){
        def fmock = new MockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        service.frameworkService = fmock.proxyInstance()
        def result = service.getFileForLocalPath("abc")
        assertNotNull(result)
        assertEquals(new File("/tmp/logs/rundeck/abc"),result)
        expect:
        // asserts validate test
        1 == 1
    }
    void testGetFileForLocalPathNotFound(){
        def fmock = new MockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever([:])
        }
        service.frameworkService = fmock.proxyInstance()
        try {
            def result = service.getFileForLocalPath("abc")
            fail("Expected exception")
        } catch (IllegalStateException e) {
            assertEquals("framework.logs.dir is not set in framework.properties", e.message)
        }
        expect:
        // asserts validate test
        1 == 1
    }
    void testCacheResult(){
        grailsApplication.config=new ConfigObject()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        service.configurationService=mockWith(ConfigurationService){
            getString{String prop,String defval->'test1'}
            getInteger(1..2){String prop,int defval->defval}
        }
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
        expect:
        // asserts validate test
        1 == 1
    }
    void testCacheResultDefaults(){
        grailsApplication.config=new ConfigObject()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        service.configurationService=mockWith(ConfigurationService){
            getString{String prop,String defval->'test1'}

            getInteger(1..2){String prop,int defval->defval}
        }
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
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testgetLogFileWriterWithoutPlugin(){
        grailsApplication.config.clear()
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def fmock = new MockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        fmock.demand.getFrameworkPropertyResolver() { project ->
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
        service.frameworkService=fmock.proxyInstance()

        def writer = service.getLogFileWriterForExecution(e, [:])
        assertNotNull(writer)
        assert writer instanceof FSStreamingLogWriter
        expect:
        // asserts validate test
        1 == 1
    }
    void testgetLogFileWriterWithFilesizeWatcher(){
        grailsApplication.config.clear()
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def fmock = new MockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        fmock.demand.getFrameworkPropertyResolver() { project ->
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
        service.frameworkService=fmock.proxyInstance()
        def test = new LoggingThreshold()
        assertNull(test.valueHolder)
        def writer = service.getLogFileWriterForExecution(e, [:],test)
        assertNotNull(test.valueHolder)
        assertEquals(0,test.valueHolder.value)
        assertNotNull(writer)
        assert writer instanceof FSStreamingLogWriter
        expect:
        // asserts validate test
        1 == 1
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
    class testOptionsStoragePlugin extends testStoragePlugin implements ExecutionFileStorageOptions{
        boolean retrieveSupported
        boolean storeSupported
    }
    class testMultiStoragePlugin extends testStoragePlugin implements ExecutionMultiFileStorage{
        Map<String,Boolean> storeMultipleResponseSet=[:]
        boolean storeMultipleCalled=false
        MultiFileStorageRequest storeMultipleFiles
        @Override
        void storeMultiple(final MultiFileStorageRequest files) throws IOException, ExecutionFileStorageException {
            storeMultipleCalled = true
            storeMultipleFiles=files
            storeMultipleResponseSet.each{k,v->
                files.storageResultForFiletype(k,v)
            }
        }
    }
    class testStoragePluginWithDelete extends testStoragePlugin{

        @Override
        boolean deleteFile(String filetype){
            testLogFile1.delete()
            return true
        }
    }
    @DirtiesRuntime
    void testgetLogFileWriterWithPluginNoRequest(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()

        def execution = createExecution()
        def writer=performWriterRequest(test, execution)

        assertNotNull(writer)
        assert writer instanceof FSStreamingLogWriter
        //context set from execution data
        assert !test.initializeCalled
        assert test.context==null

        assertEquals(0, LogFileStorageRequest.list().size())
        expect:
        // asserts validate test
        1 == 1
    }

    @DirtiesRuntime
    void testPluginLogFileWriterOnCloseShouldNotStartStorageRequest(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        LogFileStorageService svc
        def writer=performWriterRequest(test, createExecution()){LogFileStorageService service->
            svc=service
            assertEquals(0, svc.getCurrentStorageRequests().size())
        }
        writer.close()
        assertEquals(0, svc.getCurrentStorageRequests().size())
        expect:
        // asserts validate test
        1 == 1
    }

    private StreamingLogWriter performWriterRequest(testStoragePlugin test, Execution e, Closure clos=null) {
        assertNotNull(e.save())
        def fmock = new MockFor(FrameworkService)
        fmock.demand.getFrameworkProperties() {->
            PropertyResolverFactory.instanceRetriever('framework.logs.dir': '/tmp/logs')
        }
        fmock.demand.getFrameworkPropertyResolver() { project ->
            assert project == "testprojz"
        }
        def pmock = new MockFor(PluginService)
        pmock.demand.configurePlugin(2..2) { String pname, PluggableProviderService psvc, PropertyResolver resolv, PropertyScope scope ->
            assertEquals("test1", pname)
            assert scope == PropertyScope.Instance
            [instance: test, configuration: [:]]
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
        service.frameworkService = fmock.proxyInstance()
        service.pluginService = pmock.proxyInstance()

        assertEquals(0, LogFileStorageRequest.list().size())
        if(null!=clos){
            service.with(clos)
        }
        return service.getLogFileWriterForExecution(e, [:])

    }

    void testRunStorageRequestMultiSuccessSingleType(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:true]

        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertTrue(svc.executorService.executeCalled)
        assertFalse(svc.failedRequestIds.contains(task.requestId))
        expect:
        // asserts validate test
        1 == 1
    }
    void testRunStorageRequestMultiSuccessMultiType(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:true,'state.json':true]

        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog,state.json", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog','state.json'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertTrue(svc.executorService.executeCalled)
        assertTrue(!svc.failedRequestIds.contains(task.requestId))
        expect:
        // asserts validate test
        1 == 1
    }
    void testRunStorageRequestMultiSuccessStarGlob(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:true,'state.json':true,'execution.xml':true]

        LogFileStorageService svc
        Map task=performRunStorage(test, '*', createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog','state.json','execution.xml'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertTrue(svc.executorService.executeCalled)
        assertTrue(!svc.failedRequestIds.contains(task.requestId))
        expect:
        // asserts validate test
        1 == 1
    }
    void testRunStorageRequestMultiFailureSingleType(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"
        grailsApplication.config.rundeck.execution.logs.fileStorage.cancelOnStorageFailure = false

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:false]

        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0,svc.getCurrentRequests().size())

        def request
        LogFileStorageRequest.withSession { session ->
            session.flush()
            request=LogFileStorageRequest.get(task.requestId)
            request.refresh()
        }

        assertEquals(false, request.completed)
        assertEquals('rdlog', request.filetype)
        expect:
        // asserts validate test
        1 == 1
    }
    /**
     * failure of one filetype should set request filetype to the failed type(s)
     */
    void testRunStorageRequestMultiFailureMultiType(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"
        grailsApplication.config.rundeck.execution.logs.fileStorage.cancelOnStorageFailure = false

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:true,'state.json':false]

        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog,state.json", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog','state.json'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0,svc.getCurrentRequests().size())

        def request
        LogFileStorageRequest.withSession { session ->
            session.flush()
            request=LogFileStorageRequest.get(task.requestId)
            request.refresh()
        }

        assertEquals('state.json', request.filetype)
        assertFalse(request.completed)
        expect:
        // asserts validate test
        1 == 1
    }
    /**
     * failure of one filetype should set request filetype to the failed type(s)
     */
    void testRunStorageRequestMultiFailureMultiType2(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"
        grailsApplication.config.rundeck.execution.logs.fileStorage.cancelOnStorageFailure = false

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:false,'state.json':false]

        LogFileStorageService svc
        Map task=performRunStorage(test, "rdlog,state.json", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog','state.json'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0,svc.getCurrentRequests().size())

        def request
        LogFileStorageRequest.withSession { session ->
            session.flush()
            request=LogFileStorageRequest.get(task.requestId)
            request.refresh()
        }

        assertEquals('rdlog,state.json', request.filetype)
        assertFalse(request.completed)
        expect:
        // asserts validate test
        1 == 1
    }
    /**
     * failure of one filetype should set request filetype to the failed type(s)
     */
    void testRunStorageRequestMultiFailureGlobStar(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"
        grailsApplication.config.rundeck.execution.logs.fileStorage.cancelOnStorageFailure = false

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:true,'state.json':true,'execution.xml':false]

        LogFileStorageService svc
        Map task=performRunStorage(test, "*", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog','state.json','execution.xml'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0,svc.getCurrentRequests().size())

        def request
        LogFileStorageRequest.withSession { session ->
            session.flush()
            request=LogFileStorageRequest.get(task.requestId)
            request.refresh()
        }

        assertEquals('execution.xml', request.filetype)
        assertFalse(request.completed)

        expect:
        // asserts validate test
        1 == 1
    }
    /**
     * failure of one filetype should set request filetype to the failed type(s)
     */
    void testRunStorageRequestMultiFailureGlobStar2(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test2"
        grailsApplication.config.rundeck.execution.logs.fileStorage.cancelOnStorageFailure = false

        def test = new testMultiStoragePlugin()
        test.storeMultipleResponseSet=[rdlog:true,'state.json':false,'execution.xml':false]

        LogFileStorageService svc
        Map task=performRunStorage(test, "*", createExecution(), testLogFile1) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeMultipleCalled)
            assertNull(test.storeMultipleFiles)
        }

        assertTrue(test.storeMultipleCalled)
        assertNotNull(test.storeMultipleFiles)
        assertEquals(['rdlog','state.json','execution.xml'] as Set, test.storeMultipleFiles.availableFiletypes)

        assertEquals(1,task.count)
        assertFalse(svc.executorService.executeCalled)
        assertEquals(0,svc.getCurrentRequests().size())

        def request
        LogFileStorageRequest.withSession { session ->
            session.flush()
            request=LogFileStorageRequest.get(task.requestId)
            request.refresh()
        }

        assertEquals('state.json,execution.xml', request.filetype)
        assertFalse(request.completed)

        expect:
        // asserts validate test
        1 == 1
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
        assertTrue(!svc.failedRequestIds.contains(task.requestId))

        LogFileStorageRequest request
        LogFileStorageRequest.withSession { session ->
            session.flush()
            request=LogFileStorageRequest.get(task.requestId)
            request.refresh()
        }

        assertNotNull(request)
        assertTrue(request.completed)

        expect:
        // asserts validate test
        1 == 1
    }
    void testRunStorageRequestFailure(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        grailsApplication.config.rundeck.execution.logs.fileStorage.cancelOnStorageFailure = false

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

        expect:
        // asserts validate test
        1 == 1
    }
    void testRunStorageRequestFailureCancel(){
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"
        grailsApplication.config.rundeck.execution.logs.fileStorage.cancelOnStorageFailure = true

        def test = new testStoragePlugin()
        test.storeLogFileSuccess=false
        LogFileStorageService svc
        when:
        Map task=performRunStorage(test, "rdlog", createExecution(), testLogFile1){ LogFileStorageService service ->
            //default to true fo cancelOnStorageFailure
            service.configurationService=Mock(ConfigurationService){
                getBoolean(_,_)>>true
                getString(_)>>'test1'
            }
            svc = service
        }

        def request
        LogFileStorageRequest.withNewSession { session ->
            session.flush()
            request=LogFileStorageRequest.get(task.requestId)
            request?.refresh()
        }

        then:
        test.storeLogFileCalled
        test.storeFiletype == "rdlog"
        test.storeLength == testLogFile1.length()
        test.storeLastModified == new Date(testLogFile1.lastModified())
        task.count == 1
        svc.executorService.executeCalled == true

        request == null

    }
    void testRunStorageRequestFailureWithRetry(){
        given:

        def test = new testStoragePlugin()
        test.storeLogFileSuccess=false
        LogFileStorageService svc
        boolean queued=false
        def sched = new MockFor(TaskScheduler)
        sched.demand.schedule() { Closure clos, Date when ->
            queued=true
            assert when > new Date()
        }

        service.logFileStorageTaskScheduler = sched.proxyInstance()
        when:
        Map task = performRunStorage(test, "rdlog", createExecution(), testLogFile1, ['execution.logs.fileStorage.storageRetryDelay': 30, 'execution.logs.fileStorage.storageRetryCount': 2]) { LogFileStorageService service ->
            service.configurationService = Mock(ConfigurationService) {
                getInteger(_,_)>>{String value, Integer defVal->
                    if(value == "execution.logs.fileStorage.storageRetryDelay"){
                        return 30
                    }
                    if(value == "execution.logs.fileStorage.storageRetryCount"){
                        return 2
                    }
                    null
                }
                getString(_)>>"test1"
            }

            svc = service
        }


        then:
        test.storeLogFileCalled
        test.storeFiletype == "rdlog"
        test.storeLength == testLogFile1.length()
        test.storeLastModified == new Date(testLogFile1.lastModified())
        task.count == 1
        !svc.executorService.executeCalled
        svc.getCurrentRequests().size() == 0
        queued

    }
    class testProducer implements ExecutionFileProducer{
        String executionFileType
        File testfile
        boolean executionFileGenerated = false
        boolean checkpointable = false
        @Override
        ExecutionFile produceStorageFileForExecution(final ExecutionReference e) {
            new ProducedExecutionFile(localFile: testfile, fileDeletePolicy: ExecutionFile.DeletePolicy.NEVER)
        }

        @Override
        ExecutionFile produceStorageCheckpointForExecution(final ExecutionReference e) {
            produceStorageFileForExecution e
        }
    }

    private Map performRunStorage(testStoragePlugin test, String filetype, Execution e, File testfile, Closure clos = null) {
        performRunStorage(test, filetype, e, testfile, [:], clos)
    }

    private Map performRunStorage(testStoragePlugin test, String filetype, Execution e, File testfile, Map<String, Integer> intvals, Closure clos = null) {
        e.save(flush:true)

        def fmock = Mock(FrameworkService){
            //getFrameworkPropertyResolver()>>"testprojz"
        }
        //fmock.demand.getFrameworkPropertyResolver() { project ->
        //    assert project == "testprojz"
        //}
        def pmock = Mock(PluginService){
            configurePlugin("test1", _,_,PropertyScope.Instance)>>[instance: test, configuration: [:]]
        }

        def emock = new Expando()
        emock.executeCalled=false
        emock.execute={Closure cls->
            emock.executeCalled=true
            assertNotNull(cls)
            cls.call()
        }
        service.frameworkService = fmock
        service.pluginService = pmock
        service.executorService=emock
        def filetypes = filetype.split(',')
        if(filetype=='*'){
            filetypes=['rdlog','state.json','execution.xml']
        }
        Map<String,ExecutionFileProducer> loggingBeans=[:]
        for (String ftype : filetypes) {
            loggingBeans[ftype] = new testProducer(executionFileType: ftype, testfile: testfile)
        }


        def appmock = Mock(ApplicationContext){
            getBeansOfType(_)>>loggingBeans
        }
        service.applicationContext=appmock
        service.configurationService=Mock(ConfigurationService){
            //getInteger(1..3) { String prop, int defval -> intvals[prop] ?: defval }
            getBoolean(_)>>false //{String prop,boolean defval->false}
            getString(_)>>'test1' //{String prop,String defval->'test1'}
        }

        //assertEquals(0, LogFileStorageRequest.list().size())
        LogFileStorageRequest request = new LogFileStorageRequest(filetype: filetype,execution: e,pluginName:'test1',completed: false)
        request.validate()
        request.save(flush:true)
        //assertNotNull((request.errors.allErrors*.toString()).join(';'),request.save(flush:true))
        if (null != clos) {
            service.with(clos)
        }
        def task = [execId: e.id.toString(), file: testfile, storage: test, filetype: filetype,request:request,requestId:request.id]
        service.runStorageRequest(task)
        return task
    }

    @DirtiesRuntime
    void testSubmitForStorage_plugin_storeSupported(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testOptionsStoragePlugin()
        test.storeSupported=true
        service.configurationService=Mock(ConfigurationService){
            _ * getString('execution.logs.fileStoragePlugin',_)>>'test1'
        }

        Execution execution=createExecution()
        execution.save()
        prepareSubmitForStorage(test)
        service.submitForStorage(execution)

        assertEquals(1,service.storageRequests.size())
        expect:
        // asserts validate test
        1 == 1
    }

    @DirtiesRuntime
    void testSubmitForStorage_plugin_storeUnsupported(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testOptionsStoragePlugin()
        test.storeSupported=false

        Execution execution=createExecution()
        execution.save()
        prepareSubmitForStorage(test)
        service.submitForStorage(execution)

        assertEquals(0,service.storageRequests.size())

        expect:
        // asserts validate test
        1 == 1
    }


    void prepareSubmitForStorage(test){

        def fmock = Mock(FrameworkService)
        1 * fmock.getFrameworkPropertyResolverFactory('testprojz')>>Mock(PropertyResolverFactory.Factory)
        def pmock = Mock(PluginService)
        _* pmock.configurePlugin( 'test1',_, _ as PropertyResolverFactory.Factory, PropertyScope.Instance )>> new ConfiguredPlugin(test,[:])

        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        ExecutionService.metaClass.static.generateServerURL = { LinkGenerator grailsLinkGenerator ->
            ''
        }

        ExecutionService.metaClass.static.generateExecutionURL= { Execution execution1, LinkGenerator grailsLinkGenerator ->
            ''
        }
        def emock = Mock(ExecutionService){
        }

        service.frameworkService = fmock
        service.pluginService = pmock
        service.executorService=emock

    }

    @DirtiesRuntime
    void testRequestLogFileReaderFileDNE(){

        grailsApplication.config.clear()

        def test = null

        def e = createExecution()

        def reader = performReaderRequest(test, false, testLogFileDNE, false, e, false)
        assertNotNull(reader)
        assertEquals(ExecutionFileState.NOT_FOUND, reader.state)
        assertNull(reader.reader)

        expect:
        // asserts validate test
        1 == 1
    }

    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEWaiting() {

        grailsApplication.config.clear()

        def test = null

        def e = createExecution {
            it.dateStarted = new Date()
            it.outputfilepath = null
        }

        def reader = performReaderRequest(test, false, testLogFileDNE, false, e, false)
        assertNotNull(reader)
        assertEquals(ExecutionFileState.WAITING, reader.state)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEClusterModePendingRemote() {

        grailsApplication.config.clear()

        def test = new testStoragePlugin()
        test.available = false

        def e = createExecution{
            it.dateStarted = new Date()
            it.outputfilepath = null
        }

        def reader = performReaderRequest(test, true, testLogFileDNE, false, e, false)

        assertNotNull(reader)
        assertEquals(ExecutionFileState.PENDING_REMOTE, reader.state)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testRequestLogFileReaderFileExists(){

        grailsApplication.config.clear()

        def test = new testStoragePlugin()
        test.available = false

        def reader = performReaderRequest(test, false, testLogFile1, false, createExecution(), false) { svc ->

            svc.configurationService=mockWith(ConfigurationService){
                getString{String prop,String defval->null}
                getBoolean{String prop,Boolean defval->false}
            }
        }

        //initialize should not have been called
        assert !test.initializeCalled

        assert null != (reader)
        assert ExecutionFileState.AVAILABLE == reader.state
        assert null != (reader.reader)
        assert (reader.reader instanceof FSStreamingLogReader)
        expect:
        // asserts validate test
        1 == 1
    }

    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEPluginAvailableFalseShouldResultInPendingRemote() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = false

        def reader = performReaderRequest(test, false, testLogFileDNE, false, createExecution(), false)

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionFileState.PENDING_REMOTE, reader.state)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEPluginAvailableTrueShouldResultInAvailableRemote() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution(), false)

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionFileState.AVAILABLE_REMOTE, reader.state)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEPluginAvailableErrorShouldResultInError() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true
        test.availableException = true

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution(), false)

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionFileState.ERROR, reader.state)
        assertEquals('execution.log.storage.state.ERROR', reader.errorCode)
        assertEquals(['test1','testStoragePlugin.available'], reader.errorData)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEPluginRequestAlreadyPending() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true

        def e = createExecution()
        def reader = performReaderRequest(test, false, testLogFileDNE, true, e, false) { LogFileStorageService svc ->
            svc.logFileRetrievalRequests[e.id + ':rdlog'] = [state: ExecutionFileState.PENDING_LOCAL]
        }

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionFileState.PENDING_LOCAL, reader.state)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }

    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEPluginRetrieveUnsupported() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testOptionsStoragePlugin()
        test.available = true
        test.retrieveSupported=false

        def reader=performReaderRequest(test, false, testLogFileDNE, false, createExecution(), false)

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionFileState.NOT_FOUND, reader.state)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testRequestLogFileReaderFileDNEStartsANewRequest() {
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePlugin()
        test.available = true

        def LogFileStorageService service
        def reader=performReaderRequest(test, false, testLogFileDNE, true, createExecution(), false){ LogFileStorageService svc->
            service=svc
            assert svc.getCurrentRetrievalRequests().size()==0
        }
        assert service.getCurrentRetrievalRequests().size()==1

        //initialize should have been called
        assert test.initializeCalled
        assert test.context!=null

        assertNotNull(reader)
        assertEquals(ExecutionFileState.PENDING_LOCAL, reader.state)
        assertNull(reader.reader)
        expect:
        // asserts validate test
        1 == 1
    }
    @DirtiesRuntime
    void testGetFileForExecutionFiletypeLegacyLogFile() {

        grailsApplication.config.clear()

        def test = null

        def e = createExecution()
        e.outputfilepath = "/test/file/path.rdlog"

        def file = service.getFileForExecutionFiletype(e, "rdlog", true, false)
        assertNotNull(file)
        assertEquals(file, new File(e.outputfilepath))
        expect:
        // asserts validate test
        1 == 1
    }

    void testGetFileForExecutionFiletypeLegacyStateFile() {

        grailsApplication.config.clear()

        def test = null

        def e = createExecution()
        e.outputfilepath = "/test/file/path.rdlog"

        def file = service.getFileForExecutionFiletype(e, "json.state", true, false)
        assertNotNull(file)
        assertEquals(file, new File("/test/file/path.json.state"))

        expect:
        // asserts validate test
        1 == 1
    }
    void testGetFileForExecutionFileStateFile() {

        grailsApplication.config.clear()

        def test = null

        def e = createExecution()
        e.outputfilepath = "/test/file/path.rdlog"
        e.id=123

        def fwkMock = new MockFor(FrameworkService)
        fwkMock.demand.getFrameworkProperties(1..1){->
            new Properties(['framework.logs.dir':'/test2/logs'])
        }
        service.frameworkService=fwkMock.proxyInstance()

        def file = service.getFileForExecutionFiletype(e, "json.state", false, false)
        assertNotNull(file)
        assertEquals("/test2/logs/rundeck/${e.project}/run/logs/${e.id}.json.state".toString(), file.absolutePath)
        expect:
        // asserts validate test
        1 == 1
    }
    void testGetFileForExecutionFileLogFile() {

        grailsApplication.config.clear()

        def test = null

        def e = createExecution()
        e.outputfilepath = "/test/file/path.rdlog"
        e.id=123

        def fwkMock = new MockFor(FrameworkService)
        fwkMock.demand.getFrameworkProperties(1..1){->
            new Properties(['framework.logs.dir':'/test2/logs'])
        }
        service.frameworkService=fwkMock.proxyInstance()

        def file = service.getFileForExecutionFiletype(e, "rdlog", false, false)
        assertNotNull(file)
        assertEquals("/test2/logs/rundeck/${e.project}/run/logs/${e.id}.rdlog".toString(), file.absolutePath)
        expect:
        // asserts validate test
        1 == 1
    }

    void testGetFileForExecutionFileLogFileForJob() {

        grailsApplication.config.clear()

        def test = null

        def e = createJobExecution()
        e.outputfilepath = "/test/file/path.rdlog"
        e.id=123

        def fwkMock = new MockFor(FrameworkService)
        fwkMock.demand.getFrameworkProperties(1..1){->
            new Properties(['framework.logs.dir':'/test2/logs'])
        }
        service.frameworkService=fwkMock.proxyInstance()

        def file = service.getFileForExecutionFiletype(e, "rdlog", false, false)
        assertNotNull(file)
        assertEquals("/test2/logs/rundeck/${e.project}/job/test-uuid/logs/${e.id}.rdlog".toString(), file.absolutePath)
        expect:
        // asserts validate test
        1 == 1
    }

    void testGetFileForExecutionFileStateFileForJob() {

        grailsApplication.config.clear()

        def test = null

        def e = createJobExecution()
        e.outputfilepath = "/test/file/path.rdlog"
        e.id=123

        def fwkMock = new MockFor(FrameworkService)
        fwkMock.demand.getFrameworkProperties(1..1){->
            new Properties(['framework.logs.dir':'/test2/logs'])
        }
        service.frameworkService=fwkMock.proxyInstance()

        def file = service.getFileForExecutionFiletype(e, "json.state", false, false)
        assertNotNull(file)
        assertEquals("/test2/logs/rundeck/${e.project}/job/test-uuid/logs/${e.id}.json.state".toString(), file.absolutePath)
        expect:
        // asserts validate test
        1 == 1
    }



    void testGetFileForExecutionFileLegacyLogFileForJob() {

        grailsApplication.config.clear()

        def test = null

        def e = createJobExecution()
        e.outputfilepath = "/test/file/path.rdlog"
        e.id=123

        def fwkMock = new MockFor(FrameworkService)
        fwkMock.demand.getFrameworkProperties(1..1){->
            new Properties(['framework.logs.dir':'/test2/logs'])
        }
        service.frameworkService=fwkMock.proxyInstance()

        def file = service.getFileForExecutionFiletype(e, "rdlog", true, false)
        assertNotNull(file)
        assertEquals("/test/file/path.rdlog", file.absolutePath)
        expect:
        // asserts validate test
        1 == 1
    }


    void testGetFileForExecutionFileLegacyStateFileForJob() {

        grailsApplication.config.clear()

        def test = null

        def e = createJobExecution()
        e.outputfilepath = "/test/file/path.rdlog"
        e.id=123

        def fwkMock = new MockFor(FrameworkService)
        fwkMock.demand.getFrameworkProperties(1..1){->
            new Properties(['framework.logs.dir':'/test2/logs'])
        }
        service.frameworkService=fwkMock.proxyInstance()

        def file = service.getFileForExecutionFiletype(e, "json.state", true, false)
        assertNotNull(file)
        assertEquals("/test/file/path.json.state", file.absolutePath)
        expect:
        // asserts validate test
        1 == 1
    }


    private ExecutionLogReader performReaderRequest(test, boolean isClustered, File logfile, boolean performLoad, Execution e, boolean usesStoredPath, Closure svcClosure=null) {

        assertNotNull(e.save())
        def fmock = Mock(FrameworkService)
        _*fmock.isClusterModeEnabled()>> isClustered
        _ * fmock.getServerUUID() >> UUID.randomUUID()

        1 * fmock.getFrameworkPropertyResolverFactory('testprojz')>>Mock(PropertyResolverFactory.Factory)
        _ * fmock.getFrameworkProperties() >> (['framework.logs.dir': '/tmp/dir'] as Properties)

        def pmock = Mock(PluginService)
        _*pmock.configurePlugin('test1',_,_ as PropertyResolverFactory.Factory,PropertyScope.Instance)>> new ConfiguredPlugin( test, [:])

        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        ExecutionService.metaClass.static.generateServerURL = { LinkGenerator grailsLinkGenerator ->
            ''
        }

        ExecutionService.metaClass.static.generateExecutionURL= { Execution execution, LinkGenerator grailsLinkGenerator ->
            ''
        }
        service.frameworkService = fmock
//        service.frameworkService.serverUUID = null
        service.pluginService = pmock
        List useStoredValues = [true, true]
        List partialValues = [false, false]
        int useStoredNdx=0
        service.metaClass.getFileForExecutionFiletype = {
            Execution e2, String filetype, boolean useStored, boolean partial ->
                assert e == e2
                assert "rdlog"==filetype
                assert useStored==useStoredValues[useStoredNdx]
                assert partial == partialValues[useStoredNdx]
                useStoredNdx++
                logfile
        }

        service.configurationService=Mock(ConfigurationService){
            _*getString(_,_)>>'test1'
            _*getInteger(_,_)>>{it[1]}
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

    private Execution createJobExecution(Closure clos=null) {
        def se = new ScheduledExecution(
                jobName: "a job",
                groupPath: "test/group",
                uuid: "test-uuid",
                project:"tsetprojz", workflow: new Workflow(commands:[])
        )
        def e = new Execution(
                scheduledExecution: se,
                argString: "-test args", user: "testuser", project: "testprojz", loglevel: 'WARN', doNodedispatch: false)
        if(null!=clos){
            e.with(clos)
        }
        return e
    }


    private Map performDeleteStorage(testStoragePlugin test, String filetype, Execution e, File testfile, Map<String, Integer> intvals, Closure clos = null) {
        assertNotNull(e.save())
        def fmock = Mock(FrameworkService)
        1 * fmock.getFrameworkPropertyResolverFactory('testprojz')>>Mock(PropertyResolverFactory.Factory)
        def pmock = Mock(PluginService)
        _*pmock.configurePlugin('test1',_,_ as PropertyResolverFactory.Factory,PropertyScope.Instance) >> new ConfiguredPlugin<>( test,  [:])

        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        ExecutionService.metaClass.static.generateServerURL = { LinkGenerator grailsLinkGenerator ->
            ''
        }

        ExecutionService.metaClass.static.generateExecutionURL= { Execution execution1, LinkGenerator grailsLinkGenerator ->
            ''
        }
        def emock = Mock(ExecutionService)
        service.frameworkService = fmock
        service.pluginService = pmock
        service.executorService=emock
        def filetypes = filetype.split(',')
        if(filetype=='*'){
            filetypes=['rdlog','state.json','execution.xml']
        }
        Map<String,ExecutionFileProducer> loggingBeans=[:]
        for (String ftype : filetypes) {
            loggingBeans[ftype] = new testProducer(executionFileType: ftype, testfile: testfile)
        }


        service.configurationService=Mock(ConfigurationService){
            _*getString(_,_)>>'test1'
            _*getInteger(_,_)>>{it[1]}
        }

        assertEquals(0, LogFileStorageRequest.list().size())
        LogFileStorageRequest request = new LogFileStorageRequest(filetype: filetype,execution: e,pluginName:'test1',completed: false)
        request.validate()
        assertNotNull((request.errors.allErrors*.toString()).join(';'),request.save(flush:true))

        def executor = Mock(TaskExecutor)
        _*executor.execute(_)

        service.logFileStorageDeleteRemoteTask = executor

        if (null != clos) {
            service.with(clos)
        }


        return service.removeRemoteLogFile(e, filetype)
    }


    void testRemovePluginWithDelete(){
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = "test1"

        def test = new testStoragePluginWithDelete()
        test.storeLogFileSuccess=true
        test.available=true
        LogFileStorageService svc
        assertTrue(testLogFile1.exists())

        when:
        Map remove=performDeleteStorage(test, "rdlog", createExecution(), testLogFile1,[:]) { LogFileStorageService service ->
            svc = service
            assertFalse(test.storeLogFileCalled)
            assertNull(test.storeFiletype)
        }

        then:
            remove.started
        // asserts validate test
        1 == 1
    }

    void testRemoveWithoutPlugin(){
        grailsApplication.config.clear()
        Execution e = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
        assertNotNull(e.save())
        def fmock = Mock(FrameworkService)
        1 * fmock.getFrameworkPropertyResolverFactory('testproj')>>Mock(PropertyResolverFactory.Factory)
        def pmock = Mock(PluginService)

        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
        ExecutionService.metaClass.static.generateServerURL = { LinkGenerator grailsLinkGenerator ->
            ''
        }

        ExecutionService.metaClass.static.generateExecutionURL= { Execution execution1, LinkGenerator grailsLinkGenerator ->
            ''
        }
        service.frameworkService=fmock

        def result = service.removeRemoteLogFile(e,"rdlog")
        assertNotNull(result)
        assertFalse(result.started)
        assertEquals(result.error, "Not plugin enabled")
        expect:
        // asserts validate test
        1 == 1
    }

}
