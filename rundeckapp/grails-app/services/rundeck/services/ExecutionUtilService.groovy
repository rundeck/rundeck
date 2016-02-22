/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.execution.ServiceThreadBase
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import rundeck.services.logging.ExecutionLogWriter

import static org.apache.tools.ant.util.StringUtils.getStackTrace

/**
 * Non-transactional service for execution utility methods
 */
class ExecutionUtilService {
    static transactional = false
    def metricService
    def ThreadBoundOutputStream sysThreadBoundOut
    def ThreadBoundOutputStream sysThreadBoundErr

    def finishExecution(Map execMap) {
        finishExecutionMetrics(execMap)
        finishExecutionLogging(execMap)
    }
    def  finishExecutionMetrics(Map execMap) {
        def ServiceThreadBase thread = execMap.thread
        if (!thread.isSuccessful()) {
            metricService.markMeter(ExecutionService.name, 'executionFailureMeter')
        } else {
            metricService.markMeter(ExecutionService.name, 'executionSuccessMeter')
        }
    }
    def  finishExecutionLogging(Map execMap) {
        def ServiceThreadBase thread = execMap.thread
        def ExecutionLogWriter loghandler = execMap.loghandler

        try {
            if (!thread.isSuccessful()) {
                Throwable exc = thread.getThrowable()
                def errmsgs = []

                if (exc && exc instanceof com.dtolabs.rundeck.core.NodesetEmptyException) {
                    errmsgs << exc.getMessage()
                } else if (exc) {
                    errmsgs << exc.getMessage()
                    if (exc.getCause()) {
                        errmsgs << "Caused by: " + exc.getCause().getMessage()
                    }
                } else if (thread.resultObject) {
                    loghandler.logVerbose(thread.resultObject.toString())
                }
                if (errmsgs) {
                    log.error("Execution failed: " + execMap.execution.id + ": " + errmsgs.join(","))

                    loghandler.logError(errmsgs.join(','))
                    if (exc) {
                        loghandler.logVerbose(getStackTrace(exc))
                    }
                } else {
                    log.error("Execution failed: " + execMap.execution.id + ": " + thread.resultObject?.toString())
                    loghandler.logError("Execution failed: " + execMap.execution.id + ": " + thread.resultObject?.toString())
                }
            } else {
                log.info("Execution successful: " + execMap.execution.id)
            }
        } finally {
            sysThreadBoundOut.close()
            sysThreadBoundOut.removeThreadStream()
            sysThreadBoundErr.close()
            sysThreadBoundErr.removeThreadStream()
            loghandler.close()
        }
    }


}
