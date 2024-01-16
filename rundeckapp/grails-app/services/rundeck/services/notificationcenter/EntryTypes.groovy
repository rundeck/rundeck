package rundeck.services.notificationcenter

enum EntryTypes {
    TASK(0,"A time-consuming process.", "Task")

    int id
    String description
    String renderedValue

    EntryTypes(int id, String description, String renderedValue) {
        this.description = description
        this.renderedValue = renderedValue
    }

    static boolean validEntryType(String entryType){
        List<String> validEntryTypes = [
                TASK.renderedValue
        ]
        if( entryType in validEntryTypes ){
            return true
        }
        return false
    }

    static EntryType getTaskValues() {
        return new EntryType().with {
            id = TASK.id
            description = TASK.description
            renderedValue = TASK.renderedValue
            return it
        }
    }

    static EntryType resolveEntryTypeById(final int id){
        EntryType entryType = null
        switch (id){
            case 0:
                entryType = getTaskValues()
                break;
        }
        return entryType
    }

    static isValidEntryType(final int id){
        def entryTypesAvailable = [
                TASK.id
        ]

        if( !(id in entryTypesAvailable) ){
            return false
        }

        return true
    }

}