package core

databaseChangeLog = {

    changeSet(author: "greg (generated)", id: "1623967935155-3") {
        addColumn(tableName: "execution") {
            column(name: "provenance_data", type: '${text.type}')
        }
    }

}
