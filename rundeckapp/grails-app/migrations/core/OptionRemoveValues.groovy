package core

databaseChangeLog = {
    changeSet(author: "gschueler (generated)", id: "1653588208154-3") {
        comment { 'Drop rdoption_values table' }
        preConditions(onFail: 'MARK_RAN') {
            tableExists(tableName: "rdoption_values")
        }
        dropTable(tableName:'rdoption_values')
    }
}