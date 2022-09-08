package rundeck.controllers

import grails.converters.JSON
import rundeck.services.ConfigurationService

class HelplinkController {

    def configurationService
    def defaultHelplinkName = 'Get Help'

    def index() { }

    def helplinkName() {
        def userHelpLinkName = configurationService.getString("gui.helpLinkName", defaultHelplinkName)
        render(
                contentType: 'application/json', text:
                (
                        [
                                name           : userHelpLinkName,
                        ]
                ) as JSON
        )
    }

}
