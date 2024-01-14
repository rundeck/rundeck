package rundeck.services.notificationcenter

class EntryType {
    int id
    String description
    String renderedValue

    EntryType() {
    }

    EntryType(int id, String description, String renderedValue) {
        this.id = id
        this.description = description
        this.renderedValue = renderedValue
    }
}
