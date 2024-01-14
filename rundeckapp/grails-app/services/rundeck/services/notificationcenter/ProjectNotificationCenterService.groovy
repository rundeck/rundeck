package rundeck.services.notificationcenter

import grails.events.EventPublisher

class ProjectNotificationCenterService implements EventPublisher{

    def p = new ProjectNotificationCenterEntry().with {
        id = "0"
        entry_type = EntryTypes.getTaskValues()
        title = "Asynchronous Import"
        started_at = new Date()
        status = "In progress..."
        completed_proportion = "10"
        progress_proportion = "2"
        return it
    }
    def p1 = new ProjectNotificationCenterEntry().with {
        id = "1"
        entry_type = EntryTypes.getTaskValues()
        title = "Project Export"
        started_at = new Date()
        status = "In progress..."
        completed_proportion = "7"
        progress_proportion = "3"
        return it
    }

    def sampleTasks = List.of(
            p,
            p1
    )

    List<ProjectNotificationCenterEntry> getEntries(){
        return sampleTasks
    }

}
