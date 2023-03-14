package rundeck.controllers

import grails.converters.JSON

class StepDashboardController {

    def index() { }

    def renderDashboard() {
        return render(
                template: '/execution/stepsDashboard',
                model: [
                        total         : 15,
                        enabled       : 25,
                        disabled      : 35,
                ]
        )
    }

}
