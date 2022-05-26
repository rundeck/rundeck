databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1653585641550-1", dbms: 'mysql,mariadb') {
        comment { 'add primary key to workflow_workflow_step' }
        preConditions(onFail: 'MARK_RAN') {
            not {
                columnExists(tableName: 'workflow_workflow_step', columnName: 'id')
            }
        }

        addColumn(tableName: "workflow_workflow_step") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflowworkflowstepPK")
            }
        }
    }
}