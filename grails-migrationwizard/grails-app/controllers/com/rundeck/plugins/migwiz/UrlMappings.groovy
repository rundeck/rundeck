package com.rundeck.plugins.migwiz

class UrlMappings {

    static mappings = {
        "/migWiz"(controller: 'MigWiz')

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
    }
}
