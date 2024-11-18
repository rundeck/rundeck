package com.rundeck.plugins.migwiz

class UrlMappings {

    static mappings = {
        "/migWiz"(controller: 'MigWiz', action: "index")
        "/api/$api_version/priv/migWiz/migrate/$project"(controller: 'MigWiz', action: 'migrateProjectToRBA', method: 'POST')

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
    }
}
