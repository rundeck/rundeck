package rundeck.services.asyncimport

enum AsyncImportMilestone {
    M1_CREATED ("Milestone 1: Transaction Requirements.", 1),
    M2_DISTRIBUTION ("Milestone 2: Files Distribution.", 2),
    M3_IMPORTING ("Milestone 3: Files Import.", 3),
    ASYNC_IMPORT_COMPLETED ("Process completed.", 4);

    String name
    int milestoneNumber

    private AsyncImportMilestone(String name, int milestoneNumber) {
        this.name = name
        this.milestoneNumber = milestoneNumber
    }

    static validMilestoneNumber(int milestoneNumber){
        def validNumbers = [
                M1_CREATED.milestoneNumber,
                M2_DISTRIBUTION.milestoneNumber,
                M3_IMPORTING.milestoneNumber
        ]

        return validNumbers.stream().filter {number -> number == milestoneNumber}.findAny().isPresent()

    }

    public boolean equalsName(String otherName) {
        return name == otherName
    }

    public String toString() {
        return "Name: ${name}, number: ${milestoneNumber}"
    }
}