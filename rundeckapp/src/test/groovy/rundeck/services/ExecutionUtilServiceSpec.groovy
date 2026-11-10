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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    /**
     * On Java 20+ Thread.stop() always throws, which used to escape this method and
     * surface as a failure of the referencing workflow step, with the child thread orphaned.
     * It has to abandon the thread and return instead.
     */
    def "runRefJobWithTimer abandons a thread that cannot be stopped, instead of throwing"() {
        given: 'a referenced job thread that ignores the interrupt sent by abort()'
            service.metricService = Mock(MetricService)
            def thread = new InterruptIgnoringRefJobThread()
            thread.daemon = true
            thread.start()
            thread.started.await(5, TimeUnit.SECONDS)

        and: 'a stopped clock and a stop() that throws, as it does on Java 20+'
            //join() returns at once so the 100 abort waves do not take 100 seconds, and stop()
            //throws on every JDK, so this covers the fix even when the suite runs on Java 17.
            //Both are stubbed here rather than in the production code: runRefJobWithTimer is
            //dynamic Groovy, so these calls dispatch through the metaclass.
            thread.metaClass.join = { long millis -> null }
            thread.metaClass.stop = { -> throw new UnsupportedOperationException() }

        when: 'it runs past its timeout and the abort attempts are exhausted'
            def result = service.runRefJobWithTimer(thread, System.currentTimeMillis(), true, 0)

        then: 'the call returns rather than propagating UnsupportedOperationException'
            noExceptionThrown()

        and: 'the step is reported as interrupted, after the aborts were actually attempted'
            result.interrupt
            thread.abortCount >= 100

        and: 'the thread it had to abandon is counted, and is still running'
            1 * service.metricService.markMeter(
                'rundeck.services.ExecutionService',
                'referencedJobAbandonedThreadMeter'
            )
            thread.isAlive()

        cleanup:
            thread.release = true
    }

    /**
     * Stands in for the blocking plugin steps and uninterruptible IO this kill path exists to
     * handle: it swallows the interrupt that abort() sends and keeps going.
     */
    static class InterruptIgnoringRefJobThread extends WorkflowExecutionServiceThread {
        final CountDownLatch started = new CountDownLatch(1)
        volatile boolean release = false
        volatile int abortCount = 0

        InterruptIgnoringRefJobThread() {
            super(null, null, null, null, null)
        }

        @Override
        void run() {
            started.countDown()
            while (!release) {
                try {
                    Thread.sleep(20)
                } catch (InterruptedException ignored) {
                    //swallowed on purpose
                }
            }
        }

        @Override
        void abort() {
            abortCount++
            super.abort()
        }
    }
}
