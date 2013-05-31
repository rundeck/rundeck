package rundeck.controllers

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import grails.test.ControllerUnitTestCase
import rundeck.Execution
import rundeck.services.LoggingService
import rundeck.services.logging.ExecutionLogState

class ExecutionControllerTests extends ControllerUnitTestCase {

    void testDownloadOutputNotFound() {
        mockDomain(Execution)

        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath, project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def fwkControl = mockFor(LoggingService, true)
        fwkControl.demand.getLogReader { Execution e ->
            [state: ExecutionLogState.NOT_FOUND]
        }
        ec.loggingService = fwkControl.createMock()

        ec.params.id = e1.id.toString()

        def result = ec.downloadOutput()
        assertEquals(404,ec.response.status)
    }

    void testDownloadOutputNotAvailable() {
        mockDomain(Execution)

        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath, project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def fwkControl = mockFor(LoggingService, true)
        fwkControl.demand.getLogReader { Execution e ->
            [state: ExecutionLogState.AVAILABLE_REMOTE]
        }
        ec.loggingService = fwkControl.createMock()

        ec.params.id = e1.id.toString()

        def result = ec.downloadOutput()
        assertEquals(404,ec.response.status)
    }

    void testDownloadOutput(){
        mockDomain(Execution)

        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath,project:'test1',user:'bob',dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect {it.toString()}.join(",")
        assert e1.save()
        def fwkControl = mockFor(LoggingService, true)
        fwkControl.demand.getLogReader{Execution e->
            [state:ExecutionLogState.AVAILABLE,reader:new FSStreamingLogReader(tf1,"UTF-8",new RundeckLogFormat())]
        }
        ec.loggingService = fwkControl.createMock()

        ec.params.id = e1.id.toString()

        def result=ec.downloadOutput()
        assertNotNull(ec.response.getHeader('Content-Disposition'))
        assertEquals("blah blah test monkey\n" + "Execution failed on the following 1 nodes: [centos5]\n", ec.response.contentAsString)
    }

    void testDownloadOutputFormatted(){
        mockDomain(Execution)

        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath,project:'test1',user:'bob',dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect {it.toString()}.join(",")
        assert e1.save()
        def fwkControl = mockFor(LoggingService, true)
        fwkControl.demand.getLogReader { Execution e ->
            [state: ExecutionLogState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat())]
        }
        ec.loggingService = fwkControl.createMock()

        ec.params.id = e1.id.toString()
        ec.params.formatted = 'true'

        def result=ec.downloadOutput()
        assertNotNull(ec.response.getHeader('Content-Disposition'))
        def strings = ec.response.contentAsString.split("[\r\n]+") as List
        println strings
        assertEquals(["03:21:50 [admin@centos5  ][NORMAL] blah blah test monkey","03:21:51 [null@null null null][ERROR] Execution failed on the following 1 nodes: [centos5]"], strings)
    }

}
