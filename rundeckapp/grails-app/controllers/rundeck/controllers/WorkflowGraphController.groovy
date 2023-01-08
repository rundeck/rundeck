package rundeck.controllers

import grails.converters.JSON
import rundeck.services.ConfigurationService

class WorkflowGraphController {

    def configurationService
    def defaultPropValue = true

    def index() { }

    def WorkflowGraph() {
        def showGraphFlag = configurationService.getBoolean('gui.workflowGraph', defaultPropValue)

        render(
                contentType: 'application/json', text:
                (
                        [
                                showGraph : showGraphFlag,
                        ]
                ) as JSON
        )
    }

}
