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
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Integration
class ProjectDeleteLockIntegrationSpec extends Specification {

    def "project identity lookup opens its own transaction without a bound session"() {
        given:
        def project = Project.withNewTransaction {
            new Project(name: 'identity-session-project', state: RdProject.State.DISABLED)
                .save(flush: true, failOnError: true)
        }
        def method = ProjectService.getDeclaredMethod('currentProjectId', String)
        method.accessible = true
        def executor = Executors.newSingleThreadExecutor()

        when:
        def foundId = executor.submit({
            method.invoke(new ProjectService(), 'identity-session-project')
        } as Callable<Long>).get(30, TimeUnit.SECONDS)

        then:
        foundId == project.id

        cleanup:
        executor.shutdownNow()
        Project.withNewTransaction {
            Project.findByName('identity-session-project')?.delete(flush: true)
        }
    }

    def "overlapping cleanup fails within the bounded project lock timeout"() {
        given:
        def projectRow = Project.withNewSession {
            Project.withNewTransaction {
                new Project(
                    name: 'lock-test-project',
                    state: RdProject.State.DISABLED
                ).save(flush: true, failOnError: true)
            }
        }
        def blockingScmService = new BlockingScmService()
        def service = new ProjectService(
            scmService: blockingScmService,
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
            removeFrameworkProject('lock-test-project', projectRow.id) >> {
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
            service.deleteProjectInternal(project, framework, null, 'system', true, projectRow.id)
        } as Callable<ProjectService.DeleteResponse>)
        assert blockingScmService.firstCleanupEntered.await(30, TimeUnit.SECONDS)
        def second = executor.submit({
            service.deleteProjectInternal(project, framework, null, 'system', true, projectRow.id)
        } as Callable<ProjectService.DeleteResponse>)
        def secondResult = second.get(10, TimeUnit.SECONDS)
        blockingScmService.releaseFirstCleanup.countDown()
        def firstResult = first.get(30, TimeUnit.SECONDS)

        then:
        firstResult.success
        !secondResult.success
        blockingScmService.cleanupCalls.get() == 1
        Project.withNewTransaction { Project.findByName('lock-test-project') } == null

        cleanup:
        blockingScmService.releaseFirstCleanup.countDown()
        executor.shutdownNow()
        Project.withNewTransaction {
            Project.findByName('lock-test-project')?.delete(flush: true)
        }
    }

    def "overlapping finalizers cannot remove a same-name replacement"() {
        given:
        def original = Project.withNewSession {
            Project.withNewTransaction {
                new Project(name: 'finalize-lock-project', state: RdProject.State.DISABLED)
                    .save(flush: true, failOnError: true)
            }
        }
        def manager = new BlockingProjectManagerService()
        def executor = Executors.newFixedThreadPool(2)

        when:
        def first = executor.submit({
            Project.withNewSession {
                manager.finishProjectRemoval('finalize-lock-project', original.id)
            }
        } as Callable)
        assert manager.firstCleanupEntered.await(30, TimeUnit.SECONDS)
        def second = executor.submit({
            Project.withNewSession {
                manager.finishProjectRemoval('finalize-lock-project', original.id)
            }
        } as Callable)
        ExecutionException secondFailure
        try {
            second.get(10, TimeUnit.SECONDS)
        } catch (ExecutionException e) {
            secondFailure = e
        }
        manager.releaseFirstCleanup.countDown()
        first.get(30, TimeUnit.SECONDS)
        def replacement = Project.withNewTransaction {
            new Project(name: 'finalize-lock-project', state: RdProject.State.ENABLED)
                .save(flush: true, failOnError: true)
        }
        Project.withNewSession {
            manager.finishProjectRemoval('finalize-lock-project', original.id)
        }

        then:
        secondFailure
        manager.cleanupCalls.get() == 1
        Project.withNewTransaction { Project.findByName('finalize-lock-project')?.id } == replacement.id

        cleanup:
        manager.releaseFirstCleanup.countDown()
        executor.shutdownNow()
        Project.withNewTransaction {
            Project.findByName('finalize-lock-project')?.delete(flush: true)
        }
    }

    private static class BlockingProjectManagerService extends ProjectManagerService {
        final CountDownLatch firstCleanupEntered = new CountDownLatch(1)
        final CountDownLatch releaseFirstCleanup = new CountDownLatch(1)
        final AtomicInteger cleanupCalls = new AtomicInteger()

        @Override
        protected void deleteProjectResources(String projectName) {
            cleanupCalls.incrementAndGet()
            firstCleanupEntered.countDown()
            releaseFirstCleanup.await(30, TimeUnit.SECONDS)
        }
    }

    private static class BlockingScmService extends ScmService {
        final CountDownLatch firstCleanupEntered = new CountDownLatch(1)
        final CountDownLatch releaseFirstCleanup = new CountDownLatch(1)
        final AtomicInteger cleanupCalls = new AtomicInteger()

        @Override
        void unregisterAllPlugins(String project) {
            cleanupCalls.incrementAndGet()
            firstCleanupEntered.countDown()
            releaseFirstCleanup.await(30, TimeUnit.SECONDS)
        }
    }
}
