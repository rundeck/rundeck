package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecutionReference
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.authorization.AppAuthContextEvaluator
import rundeck.Execution
import rundeck.services.logging.LogFileLoader
import spock.lang.Specification

class AuthedLogFileLoaderServiceSpec extends Specification
        implements ServiceUnitTest<AuthedLogFileLoaderService>, DataTest {
    void setupSpec() {
        mockDomain Execution
    }

    void requestLogFileLoad() {
        given:
            service.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator)
            service.logFileStorageService = Mock(LogFileStorageService)
            def auth = Mock(AuthContext)
            def filetype = 'test.json'
            boolean load = true
            Execution e = new Execution(
                    argString: "-test args",
                    user: "testuser",
                    project: "TestProj",
                    loglevel: 'WARN',
                    doNodedispatch: false
            )
            e.save(flush: true)
            String eid = e.id.toString()
            def exec = Mock(ExecutionReference) {
                getId() >> eid
                getProject() >> 'TestProj'
            }
            def state = Mock(LogFileLoader)
        when:
            def result = service.requestFileLoad(auth, exec, filetype, load)
        then:
            1 * service.rundeckAuthContextEvaluator.authorizeProjectExecutionAny(auth, e, ['read', 'view']) >> true
            1 * service.logFileStorageService.requestFileLoad(exec, filetype, load) >> state
            result == state
    }

    void "service with auth"() {
        given:
            def auth = Mock(UserAndRolesAuthContext)
            def exec1 = Mock(ExecutionReference)
            service.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator)
            service.logFileStorageService = Mock(LogFileStorageService)
            def resolved = service.serviceWithAuth(auth)
            Execution e = new Execution(
                    argString: "-test args",
                    user: "testuser",
                    project: "TestProj",
                    loglevel: 'WARN',
                    doNodedispatch: false
            )
            e.save(flush: true)
            String eid = e.id.toString()
            def exec = Mock(ExecutionReference) {
                getId() >> eid
                getProject() >> 'TestProj'
            }
            def state = Mock(LogFileLoader)
        when:
            resolved.getLocalLogsDir()
        then:
            1 * service.logFileStorageService.getLocalLogsDir()
        when:
            resolved.getLocalExecutionFileForType(exec1, 'filetype')
        then:
            1 * service.logFileStorageService.getLocalExecutionFileForType(exec1, 'filetype')

        when:
            resolved.getLocalExecutionFileForType(exec1, 'filetype2', true)
        then:
            1 * service.logFileStorageService.getLocalExecutionFileForType(exec1, 'filetype2', true)

        when:


            def result = resolved.requestFileLoad(exec, 'filetype3', true)
        then:
            1 * service.rundeckAuthContextEvaluator.authorizeProjectExecutionAny(auth, e, ['read', 'view']) >> true
            1 * service.logFileStorageService.requestFileLoad(exec, 'filetype3', true) >> state
            result == state


    }
}
