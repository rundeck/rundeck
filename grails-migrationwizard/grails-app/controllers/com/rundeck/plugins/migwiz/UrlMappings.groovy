package com.rundeck.plugins.migwiz

class UrlMappings {

    static mappings = {

        "/migwiz"(action: 'index', controller: 'MigWiz')
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
    }
}
