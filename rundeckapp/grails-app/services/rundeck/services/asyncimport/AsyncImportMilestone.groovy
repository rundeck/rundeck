package rundeck.services.asyncimport

enum AsyncImportMilestone {
    M1_CREATED ("Milestone 1: Transaction creation.", 1),
    M2_DISTRIBUTION ("Milestone 2: Files distribution.", 2),
    M3_IMPORTING ("Milestone 3: Importing files to project.", 3),
    ASYNC_IMPORT_COMPLETED ("Process completed.", 4);

    String name
    int milestoneNumber

    private AsyncImportMilestone(String name, int milestoneNumber) {
        this.name = name
        this.milestoneNumber = milestoneNumber
    }

    public boolean equalsName(String otherName) {
        return name == otherName
    }

    public String toString() {
        return "Name: ${name}, number: ${milestoneNumber}"
    }
}