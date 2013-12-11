/*
 * Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>
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

import com.dtolabs.rundeck.core.execution.ServiceThreadBase
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import grails.test.GrailsUnitTestCase
import rundeck.services.logging.ExecutionLogWriter

/**
 */
class ExecutionUtilServiceTests extends GrailsUnitTestCase {

    /**
     * Finish logging when no error cause
     */
    void testFinishExecutionLoggingNoMessage() {
        mockLogging(ExecutionUtilService)
        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logError(1..1) { String value ->
            assertEquals("Execution failed: 1: null", value)
        }
        logcontrol.demand.close(1..1) {->
        }
        def loghandler = logcontrol.createMock()

        def stbocontrol = mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.removeThreadStream(2..2) {-> }
        executionUtilService.sysThreadBoundOut = stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr = stbocontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread, loghandler: loghandler, execution: [id: 1]])
    }
    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingNoMessageWithResult() {
        mockLogging(ExecutionUtilService)
        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        thread.resultObject = "abcd"
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logVerbose(1..1) { String value ->
            assertEquals("abcd", value)
        }
        logcontrol.demand.logError(1..1) { String value ->
            assertEquals("Execution failed: 1: abcd", value)
        }
        logcontrol.demand.close(1..1) {->
        }
        def loghandler = logcontrol.createMock()

        def stbocontrol = mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.removeThreadStream(2..2) {-> }
        executionUtilService.sysThreadBoundOut = stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr = stbocontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread, loghandler: loghandler, execution: [id: 1]])
    }
    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingCausedByException() {
        mockLogging(ExecutionUtilService)
        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        thread.resultObject = "abcd123"
        thread.thrown = new Exception("exceptionTest1")
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logError(1..1) { String value ->
            assertEquals("exceptionTest1", value)
        }
        logcontrol.demand.logVerbose(1..1) { String value ->
            assertNotNull(value)
            assertTrue(value.contains("exceptionTest1"))
        }
        logcontrol.demand.close(1..1) {->
        }
        def loghandler = logcontrol.createMock()

        def stbocontrol = mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.removeThreadStream(2..2) {-> }
        executionUtilService.sysThreadBoundOut = stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr = stbocontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread, loghandler: loghandler, execution: [id: 1]])
    }
    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingCausedByExceptionWithCause() {
        mockLogging(ExecutionUtilService)
        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        thread.resultObject = "abcd123"
        def cause = new Exception("exceptionCause1")
        thread.thrown = new Exception("exceptionTest1", cause)
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logError(1..1) { String value ->
            assertEquals("exceptionTest1,Caused by: exceptionCause1", value)
        }
        logcontrol.demand.logVerbose(1..1) { String value ->
            assertNotNull(value)
            assertTrue(value.contains("exceptionTest1"))
        }
        logcontrol.demand.close(1..1) {-> }
        def loghandler = logcontrol.createMock()

        def stbocontrol = mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.removeThreadStream(2..2) {-> }
        executionUtilService.sysThreadBoundOut = stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr = stbocontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread, loghandler: loghandler, execution: [id: 1]])
    }
    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingCausedByNodeSetFailureException() {
        mockLogging(ExecutionUtilService)
        def executionUtilService = new ExecutionUtilService()
        def thread = new ServiceThreadBase()
        thread.success = false
        thread.resultObject = "abcd123"
        thread.thrown = new com.dtolabs.rundeck.core.NodesetFailureException(['a', 'b'])
        def logcontrol = mockFor(ExecutionLogWriter)
        logcontrol.demand.logError(1..1) { String value ->
            assertEquals("Execution failed on the following 2 nodes: [a, b]", value)
        }
        logcontrol.demand.logVerbose(1..1) { String value ->
            assertNotNull(value)
            assertTrue(value.contains("Execution failed on the following 2 nodes"))
        }
        logcontrol.demand.close(1..1) {->
        }
        def loghandler = logcontrol.createMock()

        def stbocontrol = mockFor(ThreadBoundOutputStream)
        stbocontrol.demand.removeThreadStream(2..2) {-> }
        executionUtilService.sysThreadBoundOut = stbocontrol.createMock()
        executionUtilService.sysThreadBoundErr = stbocontrol.createMock()

        executionUtilService.finishExecutionLogging([thread: thread, loghandler: loghandler, execution: [id: 1]])
    }
}
