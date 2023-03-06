package core

databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1653665507506-1", dbms: 'mysql,mariadb') {
        comment { 'add primary key to DATABASECHANGELOG' }
        preConditions(onFail: 'MARK_RAN') {
            not {
                primaryKeyExists(tableName: 'DATABASECHANGELOG')
            }
        }
        addPrimaryKey(
            tableName: "DATABASECHANGELOG",
            columnNames: "id, author, filename",
            constraintName: "databasechangelogPK"
        )
    }
}