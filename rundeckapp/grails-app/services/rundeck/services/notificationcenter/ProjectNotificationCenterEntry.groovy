package rundeck.services.notificationcenter

class ProjectNotificationCenterEntry {
    String id
    EntryType entry_type
    String title
    String started_at
    String status
    String completed_proportion
    String progress_proportion

    ProjectNotificationCenterEntry() {
    }

    ProjectNotificationCenterEntry(
            String id,
            EntryType entry_type,
            String title,
            String started_at,
            String status,
            String completed_proportion,
            String progress_proportion
    ) {
        this.id = id
        this.entry_type = entry_type
        this.title = title
        this.started_at = started_at
        this.status = status
        this.completed_proportion = completed_proportion
        this.progress_proportion = progress_proportion
    }
}
