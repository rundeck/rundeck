package rundeck.services.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.StateExecutionFileProducer
import rundeck.Execution
import rundeck.services.LogFileStorageService
import rundeck.services.execution.ExecutionReferenceImpl
import spock.lang.Specification

class DefaultWorkflowStateDataLoaderSpec extends Specification {

    def "LoadWorkflowStateData"() {
        given:
        def loader = new DefaultWorkflowStateDataLoader()
        loader.logFileStorageService = Mock(LogFileStorageService)

        when:
        loader.loadWorkflowStateData(new ExecutionReferenceImpl(id:1L, uuid:"1234"), true)

        then:
        1 * loader.logFileStorageService.getExecutionByReferenceOrFail(_) >> new Execution(id:1L, uuid:"1234")
        1 * loader.logFileStorageService.requestLogFileLoad(_, StateExecutionFileProducer.STATE_FILE_FILETYPE, true)
    }
}
