package rundeck.services

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import spock.lang.Specification

class ScheduledExecutionServiceJobChangeEventSpec extends Specification {

    def cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
        TransactionSynchronizationManager.setActualTransactionActive(false)
    }

    private static void openTransaction() {
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)
    }

    def "inside a transaction the action waits for the commit"() {
        given:
        def ran = []
        openTransaction()

        when:
        ScheduledExecutionService.afterCommit { ran << 'published' }

        then:
        ran.isEmpty()

        when:
        TransactionSynchronizationManager.getSynchronizations()*.afterCommit()

        then:
        ran == ['published']
    }

    def "a rolled back transaction never runs the action"() {
        given:
        def ran = []
        openTransaction()

        when:
        ScheduledExecutionService.afterCommit { ran << 'published' }
        TransactionSynchronizationManager.getSynchronizations()*.afterCompletion(
            TransactionSynchronization.STATUS_ROLLED_BACK
        )

        then:
        ran.isEmpty()
    }

    def "without a transaction the action runs right away"() {
        given:
        def ran = []

        when:
        ScheduledExecutionService.afterCommit { ran << 'published' }

        then:
        ran == ['published']
    }

    def "synchronization alone, with no actual transaction, runs right away"() {
        given:
        def ran = []
        TransactionSynchronizationManager.initSynchronization()

        when:
        ScheduledExecutionService.afterCommit { ran << 'published' }

        then:
        ran == ['published']
    }
}
