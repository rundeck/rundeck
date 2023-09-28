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

    def "the execid for logstore will be the same as the Id if the output filepath is not external or take it from the outputfilepath"() {
        given:
        def e1 = new Execution(
                serverNodeUUID: 'uuid-example',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                project: "test",
                user: "user",
                status: 'true'
        )
        e1.outputfilepath = outputFilePath
        e1.id = execId

        when:
        Long returnedExecIdForStorage = e1.getExecIdForLogStore()

        then:
        returnedExecIdForStorage == expectedIdForLogStorage

        where:
        execId | outputFilePath            | expectedIdForLogStorage
        5      | 'ext:11:path/to/file.log' | 11
        5      | 'path/to/file.log'        | 5

    }
}
