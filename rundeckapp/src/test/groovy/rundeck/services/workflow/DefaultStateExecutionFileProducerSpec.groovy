package rundeck.services.workflow

import rundeck.services.WorkflowService
import rundeck.services.execution.ExecutionReferenceImpl
import spock.lang.Specification

class DefaultStateExecutionFileProducerSpec extends Specification {
    def "ProduceStorageFileForExecution"() {
        given:
        def producer = new DefaultStateExecutionFileProducer()
        producer.workflowService = Mock(WorkflowService)
        def execRef = new ExecutionReferenceImpl(id: 1L, uuid: "1234")

        when:
        producer.produceStorageFileForExecution(execRef)

        then:
        1 * producer.workflowService.getStateFileForExecution(execRef) >> new File("/tmp/1234/state.json")
    }
}
