package rundeck.services.asyncimport

enum AsyncImportMilestone {
    M1_CREATED ("Milestone 1: Transaction creation."),
    M2_DISTRIBUTION ("Milestone 2: Files distribution."),
    M3_IMPORTING ("Milestone 3: Importing files to project.");

    String name;

    private AsyncImportMilestone(String name) {
        this.name = name;
    }

    public boolean equalsName(String otherName) {
        return name == otherName;
    }

    public String toString() {
        return "Name: ${name}";
    }
}