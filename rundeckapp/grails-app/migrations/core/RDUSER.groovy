databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-16") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"rduser")
            }
        }
        createTable(tableName: "rduser") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "rduserPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "last_logged_host_name", type: '${varchar255.type}')

            column(name: "login", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "first_name", type: '${varchar255.type}')

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "last_logout", type: '${timestamp.type}')

            column(name: "filter_pref", type: '${varchar255.type}')

            column(name: "dashboard_pref", type: '${varchar255.type}')

            column(name: "PASSWORD", type: '${varchar255.type}')

            column(name: "last_session_id", type: '${varchar255.type}')

            column(name: "last_name", type: '${varchar255.type}')

            column(name: "email", type: '${varchar255.type}')

            column(name: "last_login", type: '${timestamp.type}')
        }
    }
 }