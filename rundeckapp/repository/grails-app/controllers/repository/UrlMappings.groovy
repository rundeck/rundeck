package repository

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        if(getGrailsApplication().config.rundeck.features.repository.enabled == "true") {
            "/api/$api_version/repository/list"(controller: "repository", action: "listRepositories")
            "/api/$api_version/repository/artifacts/list"(controller: "repository", action: "listArtifacts")
            "/api/$api_version/repository/artifacts/search"(controller: "repository", action: "searchArtifacts")
            "/api/$api_version/repository/$repoName/artifacts/list"(
                    controller: "repository",
                    action: "listArtifacts"
            )
            post "/api/$api_version/repository/upload"(controller: "repository", action: "uploadArtifact")
            post "/api/$api_version/repository/$repoName/upload"(
                    controller: "repository",
                    action: "uploadArtifact"
            )
            post "/api/$api_version/repository/install/$artifactId/$artifactVersion?"(
                    controller: "repository",
                    action: "installArtifact"
            )
            post "/api/$api_version/repository/$repoName/install/$artifactId/$artifactVersion?"(
                    controller: "repository",
                    action: "installArtifact"
            )
            post "/api/$api_version/repository/uninstall/$artifactId"(
                    controller: "repository",
                    action: "uninstallArtifact"
            )
            post "/api/$api_version/repository/$repoName/uninstall/$artifactId"(
                    controller: "repository",
                    action: "uninstallArtifact"
            )
            post "/api/$api_version/repository/regenerateManifest"(
                    controller: "repository",
                    action: "regenerateManifest"
            )
            post "/api/$api_version/repository/$repoName/regenerateManifest"(
                    controller: "repository",
                    action: "regenerateManifest"
            )
            post "/api/$api_version/resyncInstalledPlugins"(
                    controller: "repository",
                    action: "syncInstalledArtifactsToRundeck"
            )
            "/api/$api_version/listInstalledArtifacts"(controller: "repository", action: "listInstalledArtifacts")

            //Use plugin terminology and implicitly support 1 repo
            "/api/$api_version/plugins/types"(controller: "repository", action: "listPluginTypes")
            "/api/$api_version/plugins/list"(controller: "repository", action: "listArtifacts")
            "/api/$api_version/plugins/search"(controller: "repository", action: "searchArtifacts")
            post "/api/$api_version/plugins/upload"(controller: "repository", action: "uploadArtifact")
            post "/api/$api_version/plugins/$repoName/upload"(controller: "repository", action: "uploadArtifact")
            post "/api/$api_version/plugins/install/$artifactId/$artifactVersion?"(controller: "repository", action: "installArtifact")
            post "/api/$api_version/plugins/$repoName/install/$artifactId/$artifactVersion?"(controller: "repository", action: "installArtifact")
            post "/api/$api_version/plugins/uninstall/$artifactId"(controller: "repository", action: "uninstallArtifact")
            post "/api/$api_version/plugins/$repoName/uninstall/$artifactId"(controller: "repository", action: "uninstallArtifact")
            post "/api/$api_version/plugins/regenerateManifest"(controller: "repository", action: "regenerateManifest")
            post "/api/$api_version/plugins/resyncInstalledPlugins"(controller: "repository",action: "syncInstalledArtifactsToRundeck")
            "/api/$api_version/plugins/listInstalledArtifacts"(controller: "repository", action: "listInstalledArtifacts")

            // API endpoints used by UI
            "/repository/plugins/types"(controller: "repository", action: "listPluginTypes")
            "/repository/artifacts/list"(controller: "repository", action: "listArtifacts")
            "/repository/artifacts/search"(controller: "repository", action: "searchArtifacts")
            post "/repository/install/$artifactId/$artifactVersion?"(
                    controller: "repository",
                    action: "installArtifact"
            )
            post "/repository/${repoName}/install/$artifactId/$artifactVersion?"(
                    controller: "repository",
                    action: "installArtifact"
            )
            post "/repository/${repoName}/uninstall/$artifactId"(
                    controller: "repository",
                    action: "uninstallArtifact"
            )
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
