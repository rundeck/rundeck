databaseChangeLog = {
    changeSet(author: "Carlos Franco", id: "4.13.0-workflow-step-metadata") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "workflow", columnName: 'workflow_step_metadata')
            }
        }
        addColumn(tableName: "workflow") {
            column(name: 'workflow_step_metadata', type: '${text.type}')
        }
    }
}