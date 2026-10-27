package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import grails.events.bus.EventBus
import grails.testing.mixin.integration.Integration
import org.rundeck.app.data.model.v1.project.RdProject
import org.rundeck.app.data.providers.v1.report.ExecReportDataProvider
import rundeck.Project
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

@Integration
class ProjectDeleteLockIntegrationSpec extends Specification {

    def "overlapping cleanup waits for row deletion and skips name-based work"() {
        given:
        def projectRow = Project.withNewTransaction {
            new Project(
                name: 'lock-test-project',
                state: RdProject.State.DISABLED
            ).save(flush: true, failOnError: true)
        }
        def firstCleanupEntered = new CountDownLatch(1)
        def releaseFirstCleanup = new CountDownLatch(1)
        def cleanupCalls = new AtomicInteger()
        def service = new ProjectService(
            scmService: Mock(ScmService) {
                removeAllPluginConfiguration('lock-test-project') >> {
                    cleanupCalls.incrementAndGet()
                    firstCleanupEntered.countDown()
                    releaseFirstCleanup.await(30, TimeUnit.SECONDS)
                }
            },
            execReportDataProvider: Mock(ExecReportDataProvider),
            scheduledExecutionService: Mock(ScheduledExecutionService),
            executionService: Mock(ExecutionService) {
                deleteBulkExecutionIds([], null, 'system', true) >> [success: true]
            },
            fileUploadService: Mock(FileUploadService),
            targetEventBus: Mock(EventBus),
            componentBeanProvider: Stub(ProjectService.BeanProvider) {
                getBeans() >> [:]
            }
        )
        def manager = Mock(ProjectManager) {
            removeFrameworkProject('lock-test-project') >> {
                Project.findByName('lock-test-project').delete(flush: true, failOnError: true)
            }
        }
        def framework = Mock(Framework) {
            getFrameworkProjectMgr() >> manager
        }
        def project = Mock(IRundeckProject) {
            getName() >> 'lock-test-project'
        }
        def executor = Executors.newFixedThreadPool(2)

        when:
        def first = executor.submit({
            Project.withNewSession {
                service.deleteProjectInternal(project, framework, null, 'system', true, projectRow.id)
            }
        } as Callable<ProjectService.DeleteResponse>)
        assert firstCleanupEntered.await(30, TimeUnit.SECONDS)
        def second = executor.submit({
            Project.withNewSession {
                service.deleteProjectInternal(project, framework, null, 'system', true, projectRow.id)
            }
        } as Callable<ProjectService.DeleteResponse>)
        boolean secondWaited = false
        try {
            second.get(500, TimeUnit.MILLISECONDS)
        } catch (TimeoutException ignored) {
            secondWaited = true
        }
        releaseFirstCleanup.countDown()
        def firstResult = first.get(30, TimeUnit.SECONDS)
        def secondResult = second.get(30, TimeUnit.SECONDS)

        then:
        secondWaited
        firstResult.success
        !secondResult.success
        cleanupCalls.get() == 1
        Project.withNewTransaction { Project.findByName('lock-test-project') } == null

        cleanup:
        releaseFirstCleanup.countDown()
        executor.shutdownNow()
        Project.withNewTransaction {
            Project.findByName('lock-test-project')?.delete(flush: true)
        }
    }
}
