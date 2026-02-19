package core

databaseChangeLog = {
    // Add workflowJson column to scheduled_execution table
    changeSet(author: "rundeckuser (generated)", id: "workflow-json-storage-1") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "scheduled_execution", columnName: "workflow_json")
            }
        }
        addColumn(tableName: "scheduled_execution") {
            column(name: "workflow_json", type: '${text.type}') {
                constraints(nullable: "true")
            }
        }
    }

    // Add workflowJson column to execution table
    changeSet(author: "rundeckuser (generated)", id: "workflow-json-storage-2") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "execution", columnName: "workflow_json")
            }
        }
        addColumn(tableName: "execution") {
            column(name: "workflow_json", type: '${text.type}') {
                constraints(nullable: "true")
            }
        }
    }
}
