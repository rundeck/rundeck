package rundeck

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.validation.ValidationException
import spock.lang.Specification

@Integration
@Rollback
class ExecutionIntegrationSpec extends Specification{

    def "unique log file storage request"() {
        given:
            def uuid1 = UUID.randomUUID().toString()
            def e1 = new Execution(
                serverNodeUUID: uuid1,
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                project: "test",
                user: "user",
                status: 'true'
            ).save(flush: true,
                   failOnError: true)
            def lfsr1 = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'aplugin',
                completed: false,
                filetype: 'test'
            ).save(flush:true,
                   failOnError: true)
        when:
            def lfsr2 = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'aplugin',
                completed: true,
                filetype: 'xyz'
            ).save(flush:true,
                   failOnError: true)

        then:
            ValidationException e = thrown()

            e.errors.hasFieldErrors('execution')
            e.errors.getFieldError('execution').code=='unique'

    }
}
