databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-18") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"report_filter")
            }
        }
        createTable(tableName: "report_filter") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "report_filterPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "message_filter", type: '${varchar255.type}')

            column(name: "title_filter", type: '${varchar255.type}')

            column(name: "startafter_filter", type: '${timestamp.type}')

            column(name: "startbefore_filter", type: '${timestamp.type}')

            column(name: "job_id_filter", type: '${varchar255.type}')

            column(name: "type_filter", type: '${varchar255.type}')

            column(name: "execnode_filter", type: '${varchar255.type}')

            column(name: "recent_filter", type: '${varchar255.type}')

            column(name: "dostartafter_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "doendbefore_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "doendafter_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "report_id_filter", type: '${varchar255.type}')

            column(name: "node_filter", type: '${varchar255.type}')

            column(name: "obj_filter", type: '${varchar255.type}')

            column(name: "stat_filter", type: '${varchar255.type}')

            column(name: "cmd_filter", type: '${varchar255.type}')

            column(name: "dostartbefore_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "tags_filter", type: '${varchar255.type}')

            column(name: "user_filter", type: '${varchar255.type}')

            column(name: "name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "mapref_uri_filter", type: '${varchar255.type}')

            column(name: "proj_filter", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "endbefore_filter", type: '${timestamp.type}')

            column(name: "endafter_filter", type: '${timestamp.type}')

            column(name: "job_filter", type: '${varchar255.type}')
        }
    }
    changeSet(author: "gschueler (generated)", failOnError:"false", id: "5.0-nodefilter-1002") {
        comment { 'remove report_filter table' }
        preConditions(onFail: 'MARK_RAN') {
            tableExists (tableName:"report_filter")
        }
        dropTable(tableName: "report_filter")
    }
}