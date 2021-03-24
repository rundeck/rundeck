databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-14") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"rdoption")
            }
        }
        createTable(tableName: "rdoption") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "rdoptionPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: '${number.type}')

            column(name: "default_storage_path", type: '${varchar255.type}')

            column(name: "sort_values", type: '${boolean.type}')

            column(name: "option_type", type: '${text.type}')

            column(name: "values_url", type: '${varchar255.type}')

            column(name: "enforced", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "values_url_long", type: '${varchar3000.type}')

            column(name: "multivalued", type: '${boolean.type}')

            column(name: "delimiter", type: '${varchar255.type}')

            column(name: "values_list_delimiter", type: '${varchar255.type}')

            column(name: "secure_exposed", type: '${boolean.type}')

            column(name: "option_values_plugin_type", type: '${varchar255.type}')

            column(name: "is_date", type: '${boolean.type}')

            column(name: "default_value", type: '${text.type}')

            column(name: "config_data", type: '${text.type}')

            column(name: "sort_index", type: '${int.type}')

            column(name: "secure_input", type: '${boolean.type}')

            column(name: "hidden", type: '${boolean.type}')

            column(name: "name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "regex", type: '${text.type}')

            column(name: "required", type: '${boolean.type}')

            column(name: "date_format", type: '${varchar30.type}')

            column(name: "multivalue_all_selected", type: '${boolean.type}')

            column(name: "values_list", type: '${text.type}')

            column(name: "label", type: '${varchar255.type}')

            column(name: "description", type: '${text.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-15") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"rdoption_values")
            }
        }
        createTable(tableName: "rdoption_values") {
            column(name: "option_id", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "values_string", type: '${varchar255.type}')
        }
    }
}