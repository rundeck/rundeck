package com.rundeck.plugins.migwiz

class UrlMappings {

    static mappings = {
        "/migWiz"(controller: 'MigWiz')
        "/api/$api_version/migWiz/migrate"(controller: 'MigWiz', action: 'migrateProjectToRBA', method: 'POST')

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
    }
}
