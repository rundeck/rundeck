package verb

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
println getGrailsApplication().config.rundeck.features.verb.enabled
        if(getGrailsApplication().config.rundeck.features.verb.enabled == "true") {
println "configuring verb url mapping."
            "/api/$api_version/verb/repository/list"(controller: "repository", action: "listRepositories")
            "/api/$api_version/verb/repository/artifacts/list"(controller: "repository", action: "listArtifacts")
            "/api/$api_version/verb/repository/artifacts/search"(controller: "repository", action: "searchArtifacts")
            "/api/$api_version/verb/repository/$repoName/artifacts/list"(
                    controller: "repository",
                    action: "listArtifacts"
            )
            post "/api/$api_version/verb/repository/upload"(controller: "repository", action: "uploadArtifact")
            post "/api/$api_version/verb/repository/$repoName/upload"(
                    controller: "repository",
                    action: "uploadArtifact"
            )
            post "/api/$api_version/verb/repository/install/$artifactId/$artifactVersion?"(
                    controller: "repository",
                    action: "installArtifact"
            )
            post "/api/$api_version/verb/repository/$repoName/install/$artifactId/$artifactVersion?"(
                    controller: "repository",
                    action: "installArtifact"
            )
            post "/api/$api_version/verb/repository/uninstall/$artifactId"(
                    controller: "repository",
                    action: "uninstallArtifact"
            )
            post "/api/$api_version/verb/repository/$repoName/uninstall/$artifactId"(
                    controller: "repository",
                    action: "uninstallArtifact"
            )
            post "/api/$api_version/verb/repository/regenerateManifest"(
                    controller: "repository",
                    action: "regenerateManifest"
            )
            post "/api/$api_version/verb/repository/$repoName/regenerateManifest"(
                    controller: "repository",
                    action: "regenerateManifest"
            )
            post "/api/$api_version/verb/resyncInstalledPlugins"(
                    controller: "repository",
                    action: "syncInstalledArtifactsToRundeck"
            )
            "/api/$api_version/verb/listInstalledArtifacts"(controller: "repository", action: "listInstalledArtifacts")

            //Use plugin terminology and implicitly support 1 repo
            "/api/$api_version/plugins/list"(controller: "repository", action: "listArtifacts")
            "/api/$api_version/plugins/search"(controller: "repository", action: "searchArtifacts")
            post "/api/$api_version/plugins/upload"(controller: "repository", action: "uploadArtifact")
            post "/api/$api_version/plugins/install/$artifactId/$artifactVersion?"(controller: "repository", action: "installArtifact")
            post "/api/$api_version/plugins/uninstall/$artifactId"(controller: "repository", action: "uninstallArtifact")
            post "/api/$api_version/plugins/regenerateManifest"(controller: "repository", action: "regenerateManifest")
            post "/api/$api_version/plugins/resyncInstalledPlugins"(controller: "repository",action: "syncInstalledArtifactsToRundeck")
            "/api/$api_version/plugins/listInstalledArtifacts"(controller: "repository", action: "listInstalledArtifacts")

            // API endpoints used by UI
            "/verb/repository/artifacts/list"(controller: "repository", action: "listArtifacts")
            "/verb/repository/artifacts/search"(controller: "repository", action: "searchArtifacts")
            post "/verb/repository/install/$artifactId/$artifactVersion?"(
                    controller: "repository",
                    action: "installArtifact"
            )
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
