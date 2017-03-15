
/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class AuthFilterGrailsPlugin {
    // the plugin version
    def version = "2.0.0-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views",
        "web-app/**"
    ]

    def title = "Auth Filter Plugin" // Headline display name of the plugin
    def author = "John Stoltenborg"
    def authorEmail = ""
    def description = '''\
Adds auth filter to web.xml
'''

    // URL to the plugin's documentation
    def documentation = "http://rundeck.org"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Rundeck", url: "http://rundeck.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Greg Schueler", email: "greg@rundeck.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "Github", url: "http://github.com/dtolabs/rundeck/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://github.com/dtolabs/rundeck" ]

    def doWithWebDescriptor = { xml ->
            addFilters(application.config,xml)
    }


    def addFilters(ConfigObject config,def xml) {
        def filterNodes = xml.'context-param'
        if (filterNodes.size() > 0) {

            def filterElement = filterNodes[filterNodes.size() - 1]
            filterElement + {
                'filter' {
                    'filter-name'("AuthFilter")
                    'filter-class'("com.dtolabs.rundeck.server.filters.AuthFilter")
                }
            }


            def mappingNodes = xml.'filter'
            if (mappingNodes.size() > 0) {

                def mappingElement = mappingNodes[mappingNodes.size() - 1]
                mappingElement + {
                    'filter-mapping' {
                        'filter-name'("AuthFilter")
                        'url-pattern'("/*")
                    }
                }
            }
        }
    }
    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
