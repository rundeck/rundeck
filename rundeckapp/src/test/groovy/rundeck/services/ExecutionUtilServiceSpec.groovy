package rundeck.services


import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.workflow.ControlBehavior
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.plugins.metricsweb.MetricService
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.logging.ExecutionLogWriter
import spock.lang.Specification

import static org.junit.Assert.assertNotNull

class ExecutionUtilServiceSpec extends Specification implements ServiceUnitTest<ExecutionUtilService>, DataTest {
    def setupSpec() { mockDomains Execution, ScheduledExecution, Workflow, CommandExec }

    def testfinishExecutionMetricsSuccess() {
        given:

            service.metricService = Mock(MetricService)
            def thread = new WorkflowExecutionServiceThread(null, null, null, null, null)
            thread.success = success
        when:
            service.finishExecutionMetrics(new ExecutionService.AsyncStarted(thread: thread))

        then:
            1 * service.metricService.markMeter('rundeck.services.ExecutionService', metric)

        where:
            success | metric
            true    | 'executionSuccessMeter'
            false   | 'executionFailureMeter'
    }


    /**
     * Finish logging when no error cause
     */
    void testFinishExecutionLoggingNoMessage() {

        given:
            Execution e = new Execution(
                argString: "-test args",
                user: "testuser", project: "p1", loglevel: 'WARN',
                doNodedispatch: false,
                workflow: new Workflow(
                    commands: [new CommandExec(
                        adhocExecution: true,
                        adhocRemoteString: 'a remote string'
                    )]
                ).save()
            )
            assertNotNull(e.save())

            def thread = new WorkflowExecutionServiceThread(null, null, null, null, null)
            thread.success = false
            def loghandler = Mock(ExecutionLogWriter)

            service.configurationService = Mock(ConfigurationService)
            service.sysThreadBoundOut = new MockForThreadOutputStream(null)
            service.sysThreadBoundErr = new MockForThreadOutputStream(null)
        when:
            service.
                finishExecutionLogging(
                    new ExecutionService.AsyncStarted(
                        thread: thread,
                        loghandler: loghandler,
                        execution: e
                    )
                )

        then:
            // above asserts have validation
            1 * service.configurationService.getBoolean('execution.logs.fileStorage.generateExecutionXml', true) >>
            false
            1 * loghandler.logError("Execution failed: 1 in project p1: null")
            1 * loghandler.close()
    }


    /**
     * Finish logging when no error cause, generating execution xml
     */
    def testFinishExecutionLoggingNoMessageGenerateExecutionXml() {
        given:
            Execution e = new Execution(
                argString: "-test args",
                user: "testuser", project: "p1", loglevel: 'WARN',
                doNodedispatch: false,
                workflow: new Workflow(
                    commands: [new CommandExec(
                        adhocExecution: true,
                        adhocRemoteString: 'a remote string'
                    )]
                ).save()
            )
            assertNotNull(e.save())


            def thread = new WorkflowExecutionServiceThread(null, null, null, null, null)
            thread.success = false

            service.logFileStorageService = Mock(LogFileStorageService)


            def loghandler = Mock(ExecutionLogWriter) {
                1 * logError("Execution failed: ${e.id} in project p1: null")
                1 * close()
            }

            service.configurationService = Mock(ConfigurationService) {
                _ * getBoolean('execution.logs.fileStorage.generateExecutionXml', true) >> true
            }

            service.sysThreadBoundOut = new MockForThreadOutputStream(null)
            service.sysThreadBoundErr = new MockForThreadOutputStream(null)
        when:
            service.
                finishExecutionLogging(
                    new ExecutionService.AsyncStarted(
                        thread: thread,
                        loghandler: loghandler,
                        execution: e
                    )
                )
        then:

            1 * service.
                logFileStorageService.
                getFileForExecutionFiletype(e, ProjectService.EXECUTION_XML_LOG_FILETYPE, false) >>
            {
                return File.createTempFile("${e.id}.execution", ".xml")
            }
    }

    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingNoMessageWithResult() {
        given:
            Execution e = new Execution(
                argString: "-test args",
                user: "testuser", project: "x1", loglevel: 'WARN',
                doNodedispatch: false,
                workflow: new Workflow(
                    commands: [new CommandExec(
                        adhocExecution: true,
                        adhocRemoteString: 'a remote string'
                    )]
                ).save()
            )
            assertNotNull(e.save())
            def thread = new WorkflowExecutionServiceThread(null, null, null, null, null)
            thread.success = false
            thread.resultObject = new WorkflowExecutionResult() {
                @Override
                String getStatusString() {
                    return null
                }

                @Override
                ControlBehavior getControlBehavior() {
                    return null
                }

                @Override
                boolean isSuccess() {
                    return false
                }

                @Override
                Throwable getException() {
                    return null
                }

                @Override
                List<StepExecutionResult> getResultSet() {
                    return null
                }

                @Override
                Map<String, Collection<StepExecutionResult>> getNodeFailures() {
                    return null
                }

                @Override
                Map<Integer, StepExecutionResult> getStepFailures() {
                    return null
                }

                @Override
                String toString() {
                    "abcd"
                }

                @Override
                WFSharedContext getSharedContext() {
                    return null
                }
            }

            def loghandler = Mock(ExecutionLogWriter)

            service.configurationService = Mock(ConfigurationService) {
                1 * getBoolean('execution.logs.fileStorage.generateExecutionXml', true) >> false
            }
            service.sysThreadBoundOut = new MockForThreadOutputStream(null)
            service.sysThreadBoundErr = new MockForThreadOutputStream(null)
        when:

            service.
                finishExecutionLogging(
                    new ExecutionService.AsyncStarted(
                        thread: thread,
                        loghandler: loghandler,
                        execution: e
                    )
                )

        then:
            1 * loghandler.logVerbose('abcd')
            1 * loghandler.logError("Execution failed: ${e.id} in project x1: abcd")
            1 * loghandler.close()
    }

    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingCausedByException() {
        given:

            def thread = new WorkflowExecutionServiceThread(null, null, null, null, null)
            thread.success = false
            thread.resultObject = null
            thread.thrown = new Exception("exceptionTest1")
            def loghandler = Mock(ExecutionLogWriter)
            service.configurationService = Mock(ConfigurationService) {
                1 * getBoolean('execution.logs.fileStorage.generateExecutionXml', true) >> false
            }
            service.sysThreadBoundOut = new MockForThreadOutputStream(null)
            service.sysThreadBoundErr = new MockForThreadOutputStream(null)
        when:
            service.
                finishExecutionLogging(
                    new ExecutionService.AsyncStarted(
                        thread: thread,
                        loghandler: loghandler,
                        execution: new Execution(id: 1, project: "x1")
                    )
                )

        then:
            1 * loghandler.logError('exceptionTest1')
            1 * loghandler.logVerbose({ it.contains('exceptionTest1') })
            1 * loghandler.close()
    }


    /**
     * Finish logging when no error cause, with result
     */
    void testFinishExecutionLoggingCausedByExceptionWithCause() {
        given:

            def thread = new WorkflowExecutionServiceThread(null, null, null, null, null)
            thread.success = false
            thread.resultObject = null
            def cause = new Exception("exceptionCause1")
            thread.thrown = new Exception("exceptionTest1", cause)
            def loghandler = Mock(ExecutionLogWriter)

            service.configurationService = Mock(ConfigurationService) {
                1 * getBoolean('execution.logs.fileStorage.generateExecutionXml', true) >> false
            }
            service.sysThreadBoundOut = new MockForThreadOutputStream(null)
            service.sysThreadBoundErr = new MockForThreadOutputStream(null)

        when:
            service.
                finishExecutionLogging(
                    new ExecutionService.AsyncStarted(
                        thread: thread,
                        loghandler: loghandler,
                        execution: new Execution(id: 1, project: "x1")
                    )
                )

        then:
            1 * loghandler.logError('exceptionTest1,Caused by: exceptionCause1')
            1 * loghandler.logVerbose({ it.contains('exceptionTest1') })
            1 * loghandler.close()
    }

}
