databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-13") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"project")
            }
        }
        createTable(tableName: "project") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "projectPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "description", type: '${varchar255.type}')
        }
    }
    changeSet(author: "Alberto Hormazabal", id: "4.13.0-add-project-state") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "project", columnName: 'state')
            }
        }
        addColumn(tableName: "project") {
            column(name: 'state', type: '${varchar255.type}')
        }
    }    
}
