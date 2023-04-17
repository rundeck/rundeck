databaseChangeLog = {
    changeSet(author: "Carlos Franco", id: "4.12.0-workflow-step-metadata") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "workflow_step", columnName: 'step_metadata')
            }
        }
        addColumn(tableName: "workflow_step") {
            column(name: 'step_metadata', type: '${text.type}')
        }
    }

    changeSet(author: "Carlos Franco", id: "4.12.0-workflow-step-metadata2") {
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