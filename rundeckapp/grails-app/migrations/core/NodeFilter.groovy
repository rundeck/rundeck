databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-9") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"node_filter")
            }
        }
        createTable(tableName: "node_filter") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "node_filterPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "node_exclude_os_family", type: '${text.type}')

            column(name: "node_include_os_family", type: '${text.type}')

            column(name: "node_exclude_os_arch", type: '${text.type}')

            column(name: "node_include", type: '${text.type}')

            column(name: "node_exclude_os_version", type: '${text.type}')

            column(name: "node_exclude_precedence", type: '${boolean.type}')

            column(name: "node_exclude_name", type: '${text.type}')

            column(name: "node_include_os_version", type: '${text.type}')

            column(name: "node_exclude_os_name", type: '${text.type}')

            column(name: "filter", type: '${text.type}')

            column(name: "node_exclude_tags", type: '${text.type}')

            column(name: "node_include_tags", type: '${text.type}')

            column(name: "node_include_name", type: '${text.type}')

            column(name: "name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "node_include_os_name", type: '${text.type}')

            column(name: "node_exclude", type: '${text.type}')

            column(name: "node_include_os_arch", type: '${text.type}')

            column(name: "project", type: '${varchar255.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "3.4.0-1616620097", dbms: "h2") {
        comment { 'rename "filter" to FILTER' }
        preConditions(onFail: 'MARK_RAN') {
            grailsPrecondition {
                check {
                    def ran = sql.firstRow("SELECT count(*) as num FROM INFORMATION_SCHEMA.columns where table_name ='NODE_FILTER' and column_name  = 'filter'").num
                    if(ran==0) fail('precondition is not satisfied')
                }
            }
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE node_filter RENAME COLUMN \"filter\" TO FILTER;")
            }
            rollback {
            }
        }
    }
    changeSet(author: "gschueler (generated)", failOnError:"false", id: "5.0-nodefilter-1000") {
        comment { 'remove node_filter table' }
        preConditions(onFail: 'MARK_RAN') {
            tableExists (tableName:"node_filter")
        }
        dropTable(tableName: "node_filter") {

        }
    }
}