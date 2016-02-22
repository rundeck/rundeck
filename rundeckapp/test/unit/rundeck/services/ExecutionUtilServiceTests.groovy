package rundeck.services

import com.dtolabs.rundeck.core.execution.ServiceThreadBase
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import grails.test.mixin.*
import org.grails.plugins.metricsweb.MetricService
import org.junit.*
import rundeck.services.logging.ExecutionLogWriter

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ExecutionUtilService)
class ExecutionUtilServiceTests {

    void testfinishExecutionMetricsSuccess() {
        def executionUtilService = new ExecutionUtilService()
        def control=mockFor(MetricService)
        control.demand.markMeter(1..1){String clazz, String key->
            assertEquals('rundeck.services.ExecutionService',clazz)
            assertEquals('executionSuccessMeter',key)
        }
        executionUtilService.metricService=control.createMock()
        def thread=new ServiceThreadBase()
        thread.success=true
        executionUtilService.finishExecutionMetrics([thread:thread])
    }

    void testfinishExecutionMetricsFailure() {
        def executionUtilService = new ExecutionUtilService()
        def control=mockFor(MetricService)
        control.demand.markMeter(1..1){String clazz, String key->
            assertEquals('rundeck.services.ExecutionService',clazz)
            assertEquals('executionFailureMeter',key)
        }
        executionUtilService.metricService=control.createMock()
        def thread=new ServiceThreadBase()
        thread.success=false
        executionUtilService.finishExecutionMetrics([thread:thread])
    }

    /**
     * Finish logging when no error cause
     */
    void testFinishExecutionLoggingNoMessage(){

        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logError(1..1){String value->
            assertEquals("Execution failed: 1: null",value)
        }
        logcontrol.demand.close(1..1){->
        }
        def loghandler=logcontrol.createMock()

        def stbocontrol=mockFor(ThreadBoundOutputStream)
        def stbecontrol=mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.close(1..1){->}
        stbocontrol.demand.removeThreadStream(1..1){->}
        stbecontrol.demand.close(1..1){->}
        stbecontrol.demand.removeThreadStream(1..1){->}
        executionUtilService.sysThreadBoundOut=stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr=stbecontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread,loghandler: loghandler,execution:[id:1]])
    }
    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingNoMessageWithResult(){

        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        thread.resultObject="abcd"
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logVerbose(1..1){String value->
            assertEquals("abcd",value)
        }
        logcontrol.demand.logError(1..1){String value->
            assertEquals("Execution failed: 1: abcd",value)
        }
        logcontrol.demand.close(1..1){->
        }
        def loghandler=logcontrol.createMock()

        def stbocontrol=mockFor(ThreadBoundOutputStream)
        def stbecontrol=mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.close(1..1){->}
        stbocontrol.demand.removeThreadStream(1..1){->}
        stbecontrol.demand.close(1..1){->}
        stbecontrol.demand.removeThreadStream(1..1){->}
        executionUtilService.sysThreadBoundOut=stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr=stbecontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread,loghandler: loghandler,execution:[id:1]])
    }
    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingCausedByException(){

        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        thread.resultObject="abcd123"
        thread.thrown=new Exception("exceptionTest1")
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logError(1..1){String value->
            assertEquals("exceptionTest1",value)
        }
        logcontrol.demand.logVerbose(1..1){String value->
            assertNotNull(value)
            assertTrue(value.contains("exceptionTest1"))
        }
        logcontrol.demand.close(1..1){->
        }
        def loghandler=logcontrol.createMock()

        def stbocontrol=mockFor(ThreadBoundOutputStream)
        def stbecontrol=mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.close(1..1){->}
        stbocontrol.demand.removeThreadStream(1..1){->}
        stbecontrol.demand.close(1..1){->}
        stbecontrol.demand.removeThreadStream(1..1){->}
        executionUtilService.sysThreadBoundOut=stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr=stbecontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread,loghandler: loghandler,execution:[id:1]])
    }
    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingCausedByExceptionWithCause(){

        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        thread.resultObject="abcd123"
        def cause=new Exception("exceptionCause1")
        thread.thrown=new Exception("exceptionTest1",cause)
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logError(1..1){String value->
            assertEquals("exceptionTest1,Caused by: exceptionCause1",value)
        }
        logcontrol.demand.logVerbose(1..1){String value->
            assertNotNull(value)
            assertTrue(value.contains("exceptionTest1"))
        }
        logcontrol.demand.close(1..1){-> }
        def loghandler=logcontrol.createMock()

        def stbocontrol=mockFor(ThreadBoundOutputStream)
        def stbecontrol=mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.close(1..1){->}
        stbocontrol.demand.removeThreadStream(1..1){->}
        stbecontrol.demand.close(1..1){->}
        stbecontrol.demand.removeThreadStream(1..1){->}
        executionUtilService.sysThreadBoundOut=stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr=stbecontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread,loghandler: loghandler,execution:[id:1]])
    }
}
