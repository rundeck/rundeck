package rundeck.controllers

import com.fasterxml.jackson.databind.ObjectMapper

class EventLogController extends ControllerBase {


    def createEvent(CreateEventDto event) {
        println(event.project)

        render([ok: '200'])
    }
}

class CreateEventDto {
    String project
}